/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.examples.javaeditor;


import java.util.HashSet;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
//cmu sphinx imports

//import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * Java specific text editor.
 */
public class JavaEditor extends TextEditor {
	private IDocument mDocument;
	
	private class DefineFoldingRegionAction extends TextEditorAction {

		public DefineFoldingRegionAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
			super(bundle, prefix, editor);
		}
		
		private IAnnotationModel getAnnotationModel(ITextEditor editor) {
			return (IAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);
		}
		
		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			ITextEditor editor= getTextEditor();
			ISelection selection= editor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection= (ITextSelection) selection;
				if (!textSelection.isEmpty()) {
					IAnnotationModel model= getAnnotationModel(editor);
					if (model != null) {
						
						int start= textSelection.getStartLine();
						int end= textSelection.getEndLine();
						
						try {
							IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
							int offset= document.getLineOffset(start);
							int endOffset= document.getLineOffset(end + 1);
							Position position= new Position(offset, endOffset - offset);
							model.addAnnotation(new ProjectionAnnotation(), position);
							
						} catch (BadLocationException x) {
							// ignore
						}
					}
				}
			}
		}
	}

	/** The outline page */
	private JavaContentOutlinePage fOutlinePage;
	/** The projection support */
	private ProjectionSupport fProjectionSupport;

	/**
	 * Default constructor.
	 */
	public JavaEditor() {
		super();
//		HelloNGram.main(new String[0]);
		
		
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method extend the 
	 * actions to add those specific to the receiver
	 */
	protected void createActions() {
		super.createActions();
		
		IAction a= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS); //$NON-NLS-1$
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", a); //$NON-NLS-1$
		
		a= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistTip.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);  //$NON-NLS-1$
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		setAction("ContentAssistTip", a); //$NON-NLS-1$
		
		a= new DefineFoldingRegionAction(JavaEditorMessages.getResourceBundle(), "DefineFoldingRegion.", this); //$NON-NLS-1$
		setAction("DefineFoldingRegion", a); //$NON-NLS-1$
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * disposal actions required by the java editor.
	 */
	public void dispose() {
		if (fOutlinePage != null)
			fOutlinePage.setInput(null);
		super.dispose();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * revert behavior required by the java editor.
	 */
	public void doRevertToSaved() {
		super.doRevertToSaved();
		if (fOutlinePage != null)
			fOutlinePage.update();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * save behavior required by the java editor.
	 * 
	 * @param monitor the progress monitor
	 */
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);
		if (fOutlinePage != null)
			fOutlinePage.update();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * save as behavior required by the java editor.
	 */
	public void doSaveAs() {
		super.doSaveAs();
		if (fOutlinePage != null)
			fOutlinePage.update();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs sets the 
	 * input of the outline page after AbstractTextEditor has set input.
	 * 
	 * @param input the editor input
	 * @throws CoreException in case the input can not be set
	 */ 
	public void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (fOutlinePage != null)
			fOutlinePage.setInput(input);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ExtendedTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, "ContentAssistProposal"); //$NON-NLS-1$
		addAction(menu, "ContentAssistTip"); //$NON-NLS-1$
		addAction(menu, "DefineFoldingRegion");  //$NON-NLS-1$
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs gets
	 * the java content outline page if request is for a an 
	 * outline page.
	 * 
	 * @param required the required type
	 * @return an adapter for the required type or <code>null</code>
	 */ 
	public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			if (fOutlinePage == null) {
				fOutlinePage= new JavaContentOutlinePage(getDocumentProvider(), this);
				if (getEditorInput() != null)
					fOutlinePage.setInput(getEditorInput());
			}
			return fOutlinePage;
		}
		
		if (fProjectionSupport != null) {
			Object adapter= fProjectionSupport.getAdapter(getSourceViewer(), required);
			if (adapter != null)
				return adapter;
		}
		
		return super.getAdapter(required);
	}
		
	/* (non-Javadoc)
	 * Method declared on AbstractTextEditor
	 */
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new JavaSourceViewerConfiguration(this));
		//my additions
//		runSpeech();
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ExtendedTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
	 */
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		
		fAnnotationAccess= createAnnotationAccess();
		fOverviewRuler= createOverviewRuler(getSharedColors());
		
		ISourceViewer viewer= new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		
		return viewer;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ExtendedTextEditor#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		ProjectionViewer viewer= (ProjectionViewer) getSourceViewer();
		fProjectionSupport= new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$
		fProjectionSupport.install();
		viewer.doOperation(ProjectionViewer.TOGGLE);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#adjustHighlightRange(int, int)
	 */
	protected void adjustHighlightRange(int offset, int length) {
		ISourceViewer viewer= getSourceViewer();
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
			extension.exposeModelRange(new Region(offset, length));
		}
	}
	/**
	 * Everytime the cursor position changes
	 */
	@Override
	protected void handleCursorPositionChanged(){
		saveDoc();
		System.out.println("cursor position changed");
		IWorkbenchPage page =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart part = page.getActiveEditor();
        if(!(part instanceof AbstractTextEditor)){
        	System.out.println("here1");
        	return;
        }
        ITextEditor editor = (ITextEditor) part;
        IDocumentProvider dp = editor.getDocumentProvider();
        mDocument = dp.getDocument(editor.getEditorInput());
        System.out.println(mDocument.get());
        System.out.println("=============");
//        IContentAssistProcessor processor = getSourceViewerConfiguration().getContentAssistant(this.getSourceViewer()).getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
//        ICompletionProposal[] proposals =processor.computeCompletionProposals(this.getSourceViewer(),getOffset());
//
////        int i = 0;
//        for(ICompletionProposal p : proposals) {
//                System.out.println(p.getDisplayString());
////                i++;
////                if(i == 10)
////                        break;
//        }

        System.out.println("----------------------------");
	}
	
	private int getOffset() {
        IWorkbenchPage page =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart part = page.getActiveEditor();
        if(!(part instanceof AbstractTextEditor))
                return -1;
        ITextEditor editor = (ITextEditor) part;

        ISelection selection = editor.getSelectionProvider().getSelection();
        if(!(selection instanceof ITextSelection))
                return -1;
        return ((ITextSelection) selection).getOffset();
	}
	/**
	 * Saves the current document
	 */
	private void saveDoc() {
        IWorkbenchPage page =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart part = page.getActiveEditor();
        if(!(part instanceof AbstractTextEditor))
                return;
        if(part.isDirty()) {
                part.doSave(getProgressMonitor());
        }
	}
	
	public void insertText(String pasteText) throws BadLocationException{
		System.out.println("inserting text");
		//setup
//		IWorkbench iwb = PlatformUI.getWorkbench();
//		if (iwb == null){
//			System.err.println("IWorkbench is null");
//			return;
//		}
//		IWorkbenchWindow iww = iwb.getActiveWorkbenchWindow();
//		if(iww == null){
//			System.err.println("IWorkbenchWindow is null");
//			return;
//		}
//		IWorkbenchPage page = iww.getActivePage();
		IWorkbenchPage page = getSite().getPage();
		//IWorkbenchPage page =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page==null){
			System.err.println("Page is null");
			return;
		}
        IEditorPart part = page.getActiveEditor();
        if(!(part instanceof AbstractTextEditor)){
        	System.out.println("here1");
        	return;
        }
        ITextEditor editor = (ITextEditor) part;
        IDocumentProvider dp = editor.getDocumentProvider();
        mDocument = dp.getDocument(editor.getEditorInput());
        ITextSelection textSelection = (ITextSelection) getSite().getSelectionProvider().getSelection();
//        ITextSelection textSelection = (ITextSelection) part
//                .getSite().getSelectionProvider().getSelection();
        int offset = textSelection.getOffset(); //I think this gets the current cursor position
		mDocument.replace(offset, 0, pasteText); //puts in the text
		//move the cursor over the number of characters we just inserted
		int length = pasteText.length();
		ISelectionProvider provider= editor.getSelectionProvider();
        provider.setSelection(new TextSelection(offset+length, 0));
	}
	/**
	 * Cross references the speech string with the java completion engine
	 * @param speechWords a list of Strings consisting of the speech result
	 * Ex.
	 * <s> html which </s>
		<s> html ec </s>
		<s> html width </s>
		<s> <sil> public v </s>
		<s> <sil> public <sil> f </s>
	 * @return
	 */
	public String filterSpeechResults(String[] speechWords){
		// remove <s> </s> and </sil> tags
		for(int i =0; i<speechWords.length;i++){
			speechWords[i]=speechWords[i].replaceAll("<s>|</s>|<sil>", "").trim();
//			System.out.println(speechWords[i]);
		}
		IContentAssistProcessor processor = getSourceViewerConfiguration().getContentAssistant(this.getSourceViewer()).getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
        ICompletionProposal[] proposals =processor.computeCompletionProposals(this.getSourceViewer(),getOffset());
		// for now just print out the speechWords and java proposals to see them side by side
        
        System.out.println("==== Speech ========");
        for (String s: speechWords)
        	System.out.println(s);
        System.out.println("======= Java Proposals =======");
        for(ICompletionProposal p : proposals){
        	System.out.println(p.getDisplayString());
        	System.out.println(p.getContextInformation());
        	System.out.println(p.getAdditionalProposalInfo());
        }//end for
        
        // put all the speech proposals into a set to remove duplicates
        HashSet<String> speechWordNoDup = new HashSet<String>();
        for(String s :speechWords)
        	speechWordNoDup.add(s);
        
		return null;
	}
	
	public ICompletionProposal[] getEclipseSuggestions(){
		IContentAssistProcessor processor = getSourceViewerConfiguration().getContentAssistant(this.getSourceViewer()).getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
        ICompletionProposal[] proposals =processor.computeCompletionProposals(this.getSourceViewer(),getOffset());
		
        return proposals;
	}
	
	/*
	 * Returns the length of the longest common subsequence between two strings
	 */
	public static int lcsLength(String str1, String str2){
		if(str1.length() == 0 || str2.length() == 0)
			return 0;
		
		int[][] table = new int[str1.length()][str2.length()];
		int maxLen = 0;
		
		for(int i = 0; i < str1.length(); i++){
			for(int j = 0; j< str2.length(); j++){
				if(str1.charAt(i) != str2.charAt(j)){
					table[i][j]= 0;
				} 
				else{
					if(i==0 || j == 0)
						table[i][j] = 1;
					else
						table[i][j] = 1+table[i-1][j-1];
					if(table[i][j] > maxLen)
						maxLen = table[i][j];
				}
			} //end for
		} //end for 
		return maxLen;
	}
} // end class
