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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.Alarm
import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
import com.whisper3zzz.plugin.colorbrackets.util.RainbowColors
import java.awt.Color
import java.awt.Graphics
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
        editor.caretModel.addCaretListener(this, editor)  // tied to editor lifetime
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
        if (isExcludedLanguage(psiFile.language.id)) return

        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset) ?: return
        val container = findContainer(element) ?: return

        val range = container.textRange
        val highlighter = editor.markupModel.addRangeHighlighter(
            range.startOffset, range.endOffset,
            HighlighterLayer.SELECTION - 1,
            null,
            HighlighterTargetArea.EXACT_RANGE
        )

        val depth = getDepth(container)
        val color = RainbowColors.CURLY_BRACKETS[depth % RainbowColors.CURLY_BRACKETS.size]
        highlighter.customRenderer = ScopeLineRenderer(color)
        highlighters[editor] = highlighter
    }

    private fun isExcludedLanguage(language: String): Boolean {
        return language in EXCLUDED_LANGUAGES
    }

    private fun findContainer(element: PsiElement): PsiElement? {
        var current = element.parent
        while (current != null && current !is PsiFile) {
            if (isBlock(current)) return current
            current = current.parent
        }
        return null
    }

    private fun getDepth(element: PsiElement): Int {
        var depth = 0
        var current = element.parent
        while (current != null && current !is PsiFile) {
            if (isBlock(current)) depth++
            current = current.parent
        }
        return depth
    }

    private fun isBlock(element: PsiElement): Boolean {
        return CachedValuesManager.getCachedValue(element) {
            CachedValueProvider.Result.create(
                computeIsBlock(element),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    private fun computeIsBlock(element: PsiElement): Boolean {
        val maxScan = 50
        var hasOpen = false

        var scanner = element.firstChild
        var count = 0
        while (scanner != null && count < maxScan) {
            if (scanner.textLength == 1 && scanner.text == "{") {
                hasOpen = true
                break
            }
            scanner = scanner.nextSibling
            count++
        }
        if (!hasOpen) return false

        scanner = element.lastChild
        count = 0
        while (scanner != null && count < maxScan) {
            if (scanner.textLength == 1 && scanner.text == "}") return true
            scanner = scanner.prevSibling
            count++
        }
        return false
    }

    companion object {
        private val EXCLUDED_LANGUAGES = setOf(
            "TEXT", "PLAIN_TEXT", "Markdown", "Properties", "Shell Script",
            "Batch", "Git file", "Log", "AsciiDoc", "reStructuredText"
        )
    }
}

class ScopeLineRenderer(private val color: Color) : CustomHighlighterRenderer {
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

        val oldColor = g.color
        g.color = color
        g.drawLine(x, topY, x, bottomY)
        g.color = oldColor
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
