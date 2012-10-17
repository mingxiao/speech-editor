package org.eclipse.ui.examples.javaeditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
//import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

//import edu.cmu.sphinx.demo.hellongram.HelloNGram;
import org.eclipse.ui.examples.javaeditor.HelloNGram;

/**
 * This handler is connected to the "Editor Command" button
 * @author mingxiao10016
 *
 */
public class JavaEditorHandler extends AbstractHandler{
	private HelloNGram hng = new HelloNGram();
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// TODO Auto-generated method stub
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
        	System.out.println("editor reference: "+i.getName());
//        System.out.println("hereeee");
        
		try {
			IEditorPart tmpPart = page.openEditor(ref[0].getEditorInput(), "org.eclipse.ui.JavaEditor");
			JavaEditor t = (JavaEditor)tmpPart;
			t.getEclipseSuggestions();
//			t.insertText("inside handler");
//			HelloNGram hng = new HelloNGram();
//			String[] s = hng.record2();
//			System.out.println("you said: ");
//			for(String st : s)
//				System.out.println(st);
			
//			t.filterSpeechResults(s);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
