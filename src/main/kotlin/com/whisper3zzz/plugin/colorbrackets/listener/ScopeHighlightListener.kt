package com.whisper3zzz.plugin.colorbrackets.listener

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.RangeHighlighter as RangeHighlighterModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.Alarm
import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
import com.whisper3zzz.plugin.colorbrackets.util.BracketDepthCache
import com.whisper3zzz.plugin.colorbrackets.util.BracketKind
import com.whisper3zzz.plugin.colorbrackets.util.BracketSupport
import java.awt.Color
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.util.WeakHashMap

class ScopeHighlightActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val manager = ScopeHighlightManager(project)

        val editorFactoryListener = object : com.intellij.openapi.editor.event.EditorFactoryListener {
            override fun editorCreated(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                if (event.editor.project == project) {
                    manager.install(event.editor)
                }
            }

            override fun editorReleased(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                if (event.editor.project == project) {
                    manager.uninstall(event.editor)
                }
            }
        }

        val editorFactory = com.intellij.openapi.editor.EditorFactory.getInstance()
        editorFactory.addEditorFactoryListener(editorFactoryListener, project)

        // Register for currently open editors
        for (editor in editorFactory.allEditors) {
            if (editor.project == project) {
                manager.install(editor)
            }
        }
    }
}

class ScopeHighlightManager(private val project: Project) : CaretListener, Disposable {

    // WeakHashMap: Editor keys won't prevent GC if editor is disposed/collected
    private val highlighters = WeakHashMap<Editor, RangeHighlighter>()
    // Per-editor alarm to avoid cancelling other editors' pending updates
    private val alarms = WeakHashMap<Editor, Alarm>()

    init {
        Disposer.register(project, this)
    }

    fun install(editor: Editor) {
        editor.caretModel.removeCaretListener(this)
        editor.caretModel.addCaretListener(this, this)  // tied to manager (project) lifetime
    }

    fun uninstall(editor: Editor) {
        editor.caretModel.removeCaretListener(this)
        clearHighlighter(editor)
        highlighters.remove(editor)
        alarms.remove(editor)?.dispose()
    }

    override fun dispose() {
        // Clean up any remaining highlighters and alarms
        highlighters.forEach { (editor, _) ->
            if (!editor.isDisposed) clearHighlighter(editor)
        }
        highlighters.clear()
        alarms.values.forEach { it.dispose() }
        alarms.clear()
    }

    override fun caretPositionChanged(event: CaretEvent) {
        val editor = event.editor
        val alarm = alarms.getOrPut(editor) { Alarm(Alarm.ThreadToUse.SWING_THREAD, this) }
        alarm.cancelAllRequests()
        alarm.addRequest({
            if (!editor.isDisposed && !project.isDisposed) {
                updateHighlighter(editor)
            }
        }, 150)
    }

    private fun clearHighlighter(editor: Editor) {
        if (!editor.isDisposed) {
            highlighters[editor]?.let { editor.markupModel.removeHighlighter(it) }
        }
    }

    private fun updateHighlighter(editor: Editor) {
        if (project.isDisposed || editor.isDisposed) return

        val settings = ColorBracketsSettings.instance

        // Remove old highlighter first
        clearHighlighter(editor)
        highlighters.remove(editor)

        // Skip if plugin or scope highlight disabled
        if (!settings.isEnabled || !settings.enableScopeHighlight) return

        // Skip excluded (non-code) languages
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return

        val textLength = editor.document.textLength
        if (textLength == 0) return
        if (!BracketSupport.shouldProcessFile(psiFile.language.id, textLength, settings)) return

        val offset = editor.caretModel.offset.coerceAtMost(textLength - 1)
        val pair = BracketDepthCache.findContainingPair(psiFile, offset, BracketKind.CURLY) ?: return

        val highlighter = editor.markupModel.addRangeHighlighter(
            pair.openingOffset,
            (pair.closingOffset + 1).coerceAtMost(textLength),
            HighlighterLayer.SELECTION - 1,
            null,
            HighlighterTargetArea.EXACT_RANGE
        )

        val color = BracketSupport.colorFor(BracketKind.CURLY, pair.level, settings)
        highlighter.customRenderer = ScopeLineRenderer(
            color = color,
            width = settings.scopeLineWidth,
            opacity = settings.scopeLineOpacity
        )
        highlighters[editor] = highlighter
    }
}

class ScopeLineRenderer(
    private val color: Color,
    private val width: Int = ColorBracketsSettings.DEFAULT_SCOPE_LINE_WIDTH,
    private val opacity: Int = ColorBracketsSettings.DEFAULT_SCOPE_LINE_OPACITY
) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighterModel, g: Graphics) {
        val startOffset = highlighter.startOffset
        val endOffset = highlighter.endOffset

        val doc = editor.document
        val startLine = doc.getLineNumber(startOffset)
        val endLine = doc.getLineNumber(endOffset)

        if (startLine >= endLine) return

        val startX = getLineIndentX(editor, startLine)
        val endX = getLineIndentX(editor, endLine)
        val x = kotlin.math.min(startX, endX)

        val topY = editor.visualPositionToXY(
            editor.offsetToVisualPosition(doc.getLineStartOffset(startLine))
        ).y + editor.lineHeight
        val bottomY = editor.visualPositionToXY(
            editor.offsetToVisualPosition(doc.getLineStartOffset(endLine))
        ).y

        val g2 = g as? Graphics2D
        if (g2 == null) {
            val oldColor = g.color
            g.color = color
            g.drawLine(x, topY, x, bottomY)
            g.color = oldColor
            return
        }

        val oldColor = g2.color
        val oldStroke = g2.stroke
        val oldComposite = g2.composite
        g2.color = color
        g2.stroke = BasicStroke(width.toFloat().coerceAtLeast(1f))
        g2.composite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            opacity.coerceIn(0, 100) / 100f
        )
        g2.drawLine(x, topY, x, bottomY)
        g2.composite = oldComposite
        g2.stroke = oldStroke
        g2.color = oldColor
    }

    private fun getLineIndentX(editor: Editor, line: Int): Int {
        val doc = editor.document
        val lineStart = doc.getLineStartOffset(line)
        val lineEnd = doc.getLineEndOffset(line)
        val chars = doc.charsSequence

        var offset = lineStart
        while (offset < lineEnd) {
            if (chars[offset] != ' ' && chars[offset] != '\t') break
            offset++
        }

        return editor.visualPositionToXY(editor.offsetToVisualPosition(offset)).x
    }
}
