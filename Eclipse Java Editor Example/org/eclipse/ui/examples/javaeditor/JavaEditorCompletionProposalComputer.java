package org.eclipse.ui.examples.javaeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ui.examples.javaeditor.java.JavaProcessor;

public class JavaEditorCompletionProposalComputer implements IJavaCompletionProposalComputer {

	public void sessionStarted() {
		// TODO Auto-generated method stub
		
	}

	public List<ICompletionProposal> computeCompletionProposals(
			ContentAssistInvocationContext context, IProgressMonitor monitor) {
		
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
		
//        list.add(new CompletionProposal("Hello", 0, 0, 0));
//		list.add(new CompletionProposal("World", 0, 0, 0));
        
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
