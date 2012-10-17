package org.eclipse.ui.examples.javaeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
//import org.eclipse.jface.preference.IPreferenceNode;
//import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.text.IDocument;
//import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.examples.javaeditor.java.JavaProcessor;

/**
 * Proposal engine for the speech recognition. Relies on JavaProcessor.java 
 * (org.eclipse.ui.examples.javaeditor.java)
 * @author mingxiao10016
 *
 */
public class SpeechProposal implements IJavaCompletionProposalComputer{

	public void sessionStarted() {
		// TODO Auto-generated method stub
		
	}
	
	public JavaEditor getJavaEditor(){
		IWorkbench iwb = PlatformUI.getWorkbench();
		if (iwb == null){
			System.err.println("IWorkbench is null");
			return null;
		}
		IWorkbenchWindow iww = iwb.getActiveWorkbenchWindow();
		if(iww == null){
			System.err.println("IWorkbenchWindow is null");
			return null;
		}
		IWorkbenchPage page = iww.getActivePage();
//		IWorkbenchPage page = getSite().getPage();
		//IWorkbenchPage page =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page==null){
			System.err.println("Page is null");
			return null;
		}
//        IEditorPart part = page.getActiveEditor();
        IEditorReference[] ref = page.getEditorReferences();
        for(IEditorReference i: ref)
        	System.out.println("speech proposal: "+i.getName());
//        System.out.println("hereeee");
        
        IEditorPart tmpPart;
		try {
			tmpPart = page.openEditor(ref[0].getEditorInput(), "org.eclipse.ui.JavaEditor");
			JavaEditor t = (JavaEditor)tmpPart;
			return t;
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public List<ICompletionProposal> computeCompletionProposals(
			ContentAssistInvocationContext context, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(JavaEditorExamplePlugin.JAVA_PARTITIONING);
		assistant.setContentAssistProcessor(new JavaProcessor(assistant, IDocument.DEFAULT_CONTENT_TYPE), IDocument.DEFAULT_CONTENT_TYPE);
		IContentAssistProcessor processor = assistant.getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
		//JavaProcessor processor = new JavaProcessor(assistant, IDocument.DEFAULT_CONTENT_TYPE);
		//ICompletionProposal[] proposals = processor.computeCompletionProposals(editor.sourceViewer(), editor.getOffset());
        ICompletionProposal[] proposals = processor.computeCompletionProposals(context.getViewer(), context.getInvocationOffset());
        
        List<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
        
        for(int i = 0; i < proposals.length; i++) {
        	list.add(proposals[i]);
        }
		
       // do merge logic
        list.add(new CompletionProposal("Hello", 0, 0, 0));
		list.add(new CompletionProposal("World", 0, 0, 0));
        
		return list;
	}

	public List<IContextInformation> computeContextInformation(
			ContentAssistInvocationContext context, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	public void sessionEnded() {
		// TODO Auto-generated method stub
		
	}

}
