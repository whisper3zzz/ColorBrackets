package com.whisper3zzz.plugin.colorbrackets.listener

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.whisper3zzz.plugin.colorbrackets.util.RainbowColors
import java.awt.Color
import java.awt.Graphics
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter as RangeHighlighterModel
import com.intellij.util.Alarm

class ScopeHighlightActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val listener = ScopeHighlightManager(project)

        // Register for future editors
        val editorFactoryListener = object : com.intellij.openapi.editor.event.EditorFactoryListener {
            override fun editorCreated(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                if (event.editor.project == project) {
                    listener.install(event.editor)
                }
            }

            override fun editorReleased(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                if (event.editor.project == project) {
                    listener.uninstall(event.editor)
                }
            }
        }
        com.intellij.openapi.editor.EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, project)

        // Register for currently open editors
        for (editor in com.intellij.openapi.editor.EditorFactory.getInstance().allEditors) {
            if (editor.project == project) {
                listener.install(editor)
            }
        }
    }
}

class ScopeHighlightManager(private val project: Project) : CaretListener {
    private val highlighters = mutableMapOf<Editor, RangeHighlighter>()
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)

    fun install(editor: Editor) {
        editor.caretModel.removeCaretListener(this)
        editor.caretModel.addCaretListener(this)
    }

    fun uninstall(editor: Editor) {
        editor.caretModel.removeCaretListener(this)
        highlighters[editor]?.let { editor.markupModel.removeHighlighter(it) }
        highlighters.remove(editor)
    }

    override fun caretPositionChanged(event: CaretEvent) {
        alarm.cancelAllRequests()
        alarm.addRequest({
            updateHighlighter(event.editor)
        }, 150)
    }

    private fun updateHighlighter(editor: Editor) {
        if (project.isDisposed || editor.isDisposed) return

        val offset = editor.caretModel.offset
        // Use cached PSI if possible to avoid re-parsing on every keystroke if not committed
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return

        // Remove old highlighter
        highlighters[editor]?.let { editor.markupModel.removeHighlighter(it) }
        highlighters.remove(editor)

        val element = psiFile.findElementAt(offset) ?: return
        val container = findContainer(element) ?: return

        val range = container.textRange
        val startOffset = range.startOffset
        val endOffset = range.endOffset

        // Create a highlighter that covers the whole block
        val highlighter = editor.markupModel.addRangeHighlighter(
            startOffset, endOffset,
            HighlighterLayer.SELECTION - 1,
            null,
            HighlighterTargetArea.EXACT_RANGE
        )

        // Determine color based on depth
        val depth = getDepth(container)
        // Use depth directly to match the bracket color of the current block
        val colorIndex = depth
        val color = RainbowColors.CURLY_BRACKETS[colorIndex % RainbowColors.CURLY_BRACKETS.size]

        highlighter.customRenderer = ScopeLineRenderer(color)
        highlighters[editor] = highlighter
    }

    private fun findContainer(element: PsiElement): PsiElement? {
        var current = element.parent
        while (current != null && current !is com.intellij.psi.PsiFile) {
            if (isBlock(current)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun getDepth(element: PsiElement): Int {
        var depth = 0
        var current = element.parent
        while (current != null && current !is com.intellij.psi.PsiFile) {
            if (isBlock(current)) {
                depth++
            }
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
        // Efficiently check for { } block without loading full text
        // We look for a direct child that is a '{'
        var child = element.firstChild
        var hasOpen = false
        var hasClose = false

        // Limit the search to avoid performance issues on very large nodes with many children
        // usually brackets are at the start/end
        var count = 0
        val maxScan = 50

        // Scan from start
        var scanner = child
        while (scanner != null && count < maxScan) {
            if (scanner.textLength == 1 && scanner.text == "{") {
                hasOpen = true
                break
            }
            scanner = scanner.nextSibling
            count++
        }

        if (!hasOpen) return false

        // Scan from end
        scanner = element.lastChild
        count = 0
        while (scanner != null && count < maxScan) {
            if (scanner.textLength == 1 && scanner.text == "}") {
                hasClose = true
                break
            }
            scanner = scanner.prevSibling
            count++
        }

        return hasOpen && hasClose
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

        // Calculate X position based on the indentation of the start line
        // We want the line to align with the start of the text on the start line
        val startLineStartOffset = doc.getLineStartOffset(startLine)
        val startLineEndOffset = doc.getLineEndOffset(startLine)

        // Use charsSequence to avoid creating String objects
        val chars = doc.charsSequence
        var indentSize = 0
        for (i in startLineStartOffset until startLineEndOffset) {
            val char = chars[i]
            if (char == ' ' || char == '\t') {
                indentSize++
            } else {
                break
            }
        }

        // Calculate visual position of that indentation
        val indentVisualPos = editor.offsetToVisualPosition(startLineStartOffset + indentSize)
        val x = editor.visualPositionToXY(indentVisualPos).x

        // Draw line from startLine + 1 to endLine
        val topY = editor.visualPositionToXY(editor.offsetToVisualPosition(doc.getLineStartOffset(startLine))).y + editor.lineHeight
        val bottomY = editor.visualPositionToXY(editor.offsetToVisualPosition(doc.getLineStartOffset(endLine))).y

        val oldColor = g.color
        g.color = color
        // Draw a slightly thicker line or just 1px
        g.drawLine(x, topY, x, bottomY)
        g.color = oldColor
    }
}
