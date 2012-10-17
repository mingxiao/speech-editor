package org.eclipse.ui.examples.javaeditor.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalCategory;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;
import org.eclipse.jdt.internal.ui.text.java.ProposalSorterHandle;
import org.eclipse.jdt.internal.ui.text.java.ProposalSorterRegistry;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Java completion processor.
 */
public class JavaProcessor implements IContentAssistProcessor {

	/**
	 * The completion listener class for this processor.
	 *
	 * @since 3.4
	 */
	private final class CompletionListener implements ICompletionListener, ICompletionListenerExtension {
		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionListener#assistSessionStarted(org.eclipse.jface.text.contentassist.ContentAssistEvent)
		 */
		public void assistSessionStarted(ContentAssistEvent event) {
			if (event.processor != JavaProcessor.this)
				return;

			fIterationGesture= getIterationGesture();
			KeySequence binding= getIterationBinding();

			// This may show the warning dialog if all categories are disabled
			setCategoryIteration();
			for (Iterator<CompletionProposalCategory> it= fCategories.iterator(); it.hasNext();) {
				CompletionProposalCategory cat= it.next();
				cat.sessionStarted();
			}

			fRepetition= 0;
			if (event.assistant instanceof IContentAssistantExtension2) {
				IContentAssistantExtension2 extension= (IContentAssistantExtension2) event.assistant;

				if (fCategoryIteration.size() == 1) {
					extension.setRepeatedInvocationMode(false);
					extension.setShowEmptyList(false);
				} else {
					extension.setRepeatedInvocationMode(true);
					extension.setStatusLineVisible(true);
					extension.setStatusMessage(createIterationMessage());
					extension.setShowEmptyList(true);
					if (extension instanceof IContentAssistantExtension3) {
						IContentAssistantExtension3 ext3= (IContentAssistantExtension3) extension;
						((ContentAssistant) ext3).setRepeatedInvocationTrigger(binding);
					}
				}

			}
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionListener#assistSessionEnded(org.eclipse.jface.text.contentassist.ContentAssistEvent)
		 */
		public void assistSessionEnded(ContentAssistEvent event) {
			if (event.processor != JavaProcessor.this)
				return;

			for (Iterator<CompletionProposalCategory> it= fCategories.iterator(); it.hasNext();) {
				CompletionProposalCategory cat= it.next();
				cat.sessionEnded();
			}

			fCategoryIteration= null;
			fRepetition= -1;
			fIterationGesture= null;
			if (event.assistant instanceof IContentAssistantExtension2) {
				IContentAssistantExtension2 extension= (IContentAssistantExtension2) event.assistant;
				extension.setShowEmptyList(false);
				extension.setRepeatedInvocationMode(false);
				extension.setStatusLineVisible(false);
				if (extension instanceof IContentAssistantExtension3) {
					IContentAssistantExtension3 ext3= (IContentAssistantExtension3) extension;
					((ContentAssistant) ext3).setRepeatedInvocationTrigger(null);
				}
			}
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionListener#selectionChanged(org.eclipse.jface.text.contentassist.ICompletionProposal, boolean)
		 */
		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionListenerExtension#assistSessionRestarted(org.eclipse.jface.text.contentassist.ContentAssistEvent)
		 * @since 3.4
		 */
		public void assistSessionRestarted(ContentAssistEvent event) {
			fRepetition= 0;
		}
	}

	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/ResultCollector"));  //$NON-NLS-1$//$NON-NLS-2$

	/**
	 * Dialog settings key for the "all categories are disabled" warning dialog. See
	 * {@link OptionalMessageDialog}.
	 *
	 * @since 3.3
	 */
	private static final String PREF_WARN_ABOUT_EMPTY_ASSIST_CATEGORY= "EmptyDefaultAssistCategory"; //$NON-NLS-1$

	private static final Comparator<CompletionProposalCategory> ORDER_COMPARATOR= new Comparator<CompletionProposalCategory>() {

		public int compare(CompletionProposalCategory d1, CompletionProposalCategory d2) {
			return d1.getSortOrder() - d2.getSortOrder();
		}

	};

	private final List<CompletionProposalCategory> fCategories;
	private final String fPartition;
	private final ContentAssistant fAssistant;

	private char[] fCompletionAutoActivationCharacters;

	/* cycling stuff */
	private int fRepetition= -1;
	private List<List<CompletionProposalCategory>> fCategoryIteration= null;
	private String fIterationGesture= null;
	private int fNumberOfComputedResults= 0;
	private String fErrorMessage;

	/**
	 * The completion proposal registry.
	 *
	 * @since 3.4
	 */
	private CompletionProposalComputerRegistry fComputerRegistry;

	/**
	 * Flag indicating whether any completion engine associated with this processor requests
	 * resorting of its proposals after filtering is triggered. Filtering is, e.g., triggered when a
	 * user continues typing with an open completion window.
	 * 
	 * @since 3.8
	 */
	private boolean fNeedsSortingAfterFiltering;


	public JavaProcessor(ContentAssistant assistant, String partition) {
		Assert.isNotNull(partition);
		Assert.isNotNull(assistant);
		fPartition= partition;
		fComputerRegistry= CompletionProposalComputerRegistry.getDefault();
		fCategories= fComputerRegistry.getProposalCategories();
		fAssistant= assistant;
		fAssistant.addCompletionListener(new CompletionListener());
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public final ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		long start= DEBUG ? System.currentTimeMillis() : 0;

		clearState();

		IProgressMonitor monitor= createProgressMonitor();
		//monitor.beginTask(JavaTextMessages.ContentAssistProcessor_computing_proposals, fCategories.size() + 1);

		ContentAssistInvocationContext context= createContext(viewer, offset);
		long setup= DEBUG ? System.currentTimeMillis() : 0;

		//monitor.subTask(JavaTextMessages.ContentAssistProcessor_collecting_proposals);
		List<ICompletionProposal> proposals= collectProposals(viewer, offset, monitor, context);
		long collect= DEBUG ? System.currentTimeMillis() : 0;

		//monitor.subTask(JavaTextMessages.ContentAssistProcessor_sorting_proposals);
		if (fNeedsSortingAfterFiltering)
			setContentAssistSorter();
		else
			proposals= sortProposals(proposals, monitor, context);
		fNumberOfComputedResults= proposals.size();
		long filter= DEBUG ? System.currentTimeMillis() : 0;

		ICompletionProposal[] result= proposals.toArray(new ICompletionProposal[proposals.size()]);
		monitor.done();

		if (DEBUG) {
			System.err.println("Code Assist Stats (" + result.length + " proposals)"); //$NON-NLS-1$ //$NON-NLS-2$
			System.err.println("Code Assist (setup):\t" + (setup - start) ); //$NON-NLS-1$
			System.err.println("Code Assist (collect):\t" + (collect - setup) ); //$NON-NLS-1$
			System.err.println("Code Assist (sort):\t" + (filter - collect) ); //$NON-NLS-1$
		}

		return result;
	}

	private void clearState() {
		fErrorMessage=null;
		fNumberOfComputedResults= 0;
	}

	/**
	 * Collects the proposals.
	 *
	 * @param viewer the text viewer
	 * @param offset the offset
	 * @param monitor the progress monitor
	 * @param context the code assist invocation context
	 * @return the list of proposals
	 */
	private List<ICompletionProposal> collectProposals(ITextViewer viewer, int offset, IProgressMonitor monitor, ContentAssistInvocationContext context) {
		List<ICompletionProposal> proposals= new ArrayList<ICompletionProposal>();
		List<CompletionProposalCategory> providers= getCategories();
		for (Iterator<CompletionProposalCategory> it= providers.iterator(); it.hasNext();) {
			CompletionProposalCategory cat= it.next();
			
			System.out.println(cat.getId() + " " + cat.getDisplayName());
			//only want the default proposals to be considered. So we don't run into the loop
			if(cat.getId().equals("org.eclipse.jdt.ui.javaAllProposalCategory")) {
				List<ICompletionProposal> computed= cat.computeCompletionProposals(context, fPartition, new SubProgressMonitor(monitor, 1));
				proposals.addAll(computed);
				//needsSortingAfterFiltering= needsSortingAfterFiltering || (cat.isSortingAfterFilteringNeeded() && !computed.isEmpty());
				if (fErrorMessage == null)
					fErrorMessage= cat.getErrorMessage();
			}
		}
//		if (fNeedsSortingAfterFiltering && !needsSortingAfterFiltering)
//			fAssistant.setSorter(null);
//		fNeedsSortingAfterFiltering= needsSortingAfterFiltering;
		return proposals;
	}

	/**
	 * Filters and sorts the proposals. The passed list may be modified
	 * and returned, or a new list may be created and returned.
	 *
	 * @param proposals the list of collected proposals (element type:
	 *        {@link ICompletionProposal})
	 * @param monitor a progress monitor
	 * @param context TODO
	 * @return the list of filtered and sorted proposals, ready for
	 *         display (element type: {@link ICompletionProposal})
	 */
	protected List<ICompletionProposal> sortProposals(List<ICompletionProposal> proposals, IProgressMonitor monitor, ContentAssistInvocationContext context) {
		return proposals;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		clearState();

		IProgressMonitor monitor= createProgressMonitor();
		//monitor.beginTask(JavaTextMessages.ContentAssistProcessor_computing_contexts, fCategories.size() + 1);

		//monitor.subTask(JavaTextMessages.ContentAssistProcessor_collecting_contexts);
		List<IContextInformation> proposals= collectContextInformation(viewer, offset, monitor);

		//monitor.subTask(JavaTextMessages.ContentAssistProcessor_sorting_contexts);
		List<IContextInformation> filtered= filterAndSortContextInformation(proposals, monitor);
		fNumberOfComputedResults= filtered.size();

		IContextInformation[] result= filtered.toArray(new IContextInformation[filtered.size()]);
		monitor.done();
		return result;
	}

	private List<IContextInformation> collectContextInformation(ITextViewer viewer, int offset, IProgressMonitor monitor) {
		List<IContextInformation> proposals= new ArrayList<IContextInformation>();
		ContentAssistInvocationContext context= createContext(viewer, offset);

		List<CompletionProposalCategory> providers= getCategories();
		for (Iterator<CompletionProposalCategory> it= providers.iterator(); it.hasNext();) {
			CompletionProposalCategory cat= it.next();
			List<IContextInformation> computed= cat.computeContextInformation(context, fPartition, new SubProgressMonitor(monitor, 1));
			proposals.addAll(computed);
			if (fErrorMessage == null)
				fErrorMessage= cat.getErrorMessage();
		}

		return proposals;
	}

	/**
	 * Filters and sorts the context information objects. The passed
	 * list may be modified and returned, or a new list may be created
	 * and returned.
	 *
	 * @param contexts the list of collected proposals (element type:
	 *        {@link IContextInformation})
	 * @param monitor a progress monitor
	 * @return the list of filtered and sorted proposals, ready for
	 *         display (element type: {@link IContextInformation})
	 */
	protected List<IContextInformation> filterAndSortContextInformation(List<IContextInformation> contexts, IProgressMonitor monitor) {
		return contexts;
	}

	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 *
	 * @param activationSet the activation set
	 */
	public final void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
		fCompletionAutoActivationCharacters= activationSet;
	}


	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public final char[] getCompletionProposalAutoActivationCharacters() {
		return fCompletionAutoActivationCharacters;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		if (fErrorMessage != null)
			return fErrorMessage;
		if (fNumberOfComputedResults > 0)
			return null;
		return JavaUIMessages.JavaEditor_codeassist_noCompletions;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/**
	 * Creates a progress monitor.
	 * <p>
	 * The default implementation creates a
	 * <code>NullProgressMonitor</code>.
	 * </p>
	 *
	 * @return a progress monitor
	 */
	protected IProgressMonitor createProgressMonitor() {
		return new NullProgressMonitor();
	}

	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 *
	 * @param viewer the viewer that content assist is invoked on
	 * @param offset the content assist offset
	 * @return the context to be passed to the computers
	 */
	protected ContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
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
		if (page==null){
			System.err.println("Page is null");
			return null;
		}
        IEditorPart part = page.getActiveEditor();
        if(!(part instanceof AbstractTextEditor)) {
			System.err.println("Part not instance of Abstract Test Editor");
        	return null;
        }
		
		return new JavaContentAssistInvocationContext(viewer, offset, part);
	}

	private List<CompletionProposalCategory> getCategories() {
		if (fCategoryIteration == null)
			return fCategories;

		int iteration= fRepetition % fCategoryIteration.size();
		fAssistant.setStatusMessage(createIterationMessage());
		fAssistant.setEmptyMessage(createEmptyMessage());
		fRepetition++;

//		fAssistant.setShowMessage(fRepetition % 2 != 0);

		return fCategoryIteration.get(iteration);
	}

	// This may show the warning dialog if all categories are disabled
	private void setCategoryIteration() {
		fCategoryIteration= getCategoryIteration();
	}

	private List<List<CompletionProposalCategory>> getCategoryIteration() {
		List<List<CompletionProposalCategory>> sequence= new ArrayList<List<CompletionProposalCategory>>();
		sequence.add(getDefaultCategories());
		for (Iterator<CompletionProposalCategory> it= getSeparateCategories().iterator(); it.hasNext();) {
			CompletionProposalCategory cat= it.next();
			sequence.add(Collections.singletonList(cat));
		}
		return sequence;
	}

	private List<CompletionProposalCategory> getDefaultCategories() {
		// default mix - enable all included computers
		List<CompletionProposalCategory> included= getDefaultCategoriesUnchecked();

//		if (fComputerRegistry.hasUninstalledComputers(fPartition, included)) {
//			if (informUserAboutEmptyDefaultCategory())
//				// preferences were restored - recompute the default categories
//				included= getDefaultCategoriesUnchecked();
//			fComputerRegistry.resetUnistalledComputers();
//		}

		return included;
	}

	private List<CompletionProposalCategory> getDefaultCategoriesUnchecked() {
		List<CompletionProposalCategory> included= new ArrayList<CompletionProposalCategory>();
		for (Iterator<CompletionProposalCategory> it= fCategories.iterator(); it.hasNext();) {
			CompletionProposalCategory category= it.next();
			if (checkDefaultEnablement(category)) 
				included.add(category);
		}
		return included;
	}

	/**
	 * Determine whether the category is enabled by default.
	 * 
	 * @param category the category to check
	 * @return <code>true</code> if this category is enabled by default, <code>false</code>
	 *         otherwise
	 * @since 3.8
	 */
	protected boolean checkDefaultEnablement(CompletionProposalCategory category) {
		return category.isIncluded() && category.hasComputers(fPartition);
	}

	private List<CompletionProposalCategory> getSeparateCategories() {
		ArrayList<CompletionProposalCategory> sorted= new ArrayList<CompletionProposalCategory>();
		for (Iterator<CompletionProposalCategory> it= fCategories.iterator(); it.hasNext();) {
			CompletionProposalCategory category= it.next();
			if (checkSeparateEnablement(category))
				sorted.add(category);
		}
		Collections.sort(sorted, ORDER_COMPARATOR);
		return sorted;
	}
	
	/**
	 * Determine whether the category is enabled for separate use.
	 * 
	 * @param category the category to check
	 * @return <code>true</code> if this category is enabled for separate use, <code>false</code>
	 *         otherwise
	 * @since 3.8
	 */
	protected boolean checkSeparateEnablement(CompletionProposalCategory category) {
		return category.isSeparateCommand() && category.hasComputers(fPartition);
	}

	private String createEmptyMessage() {
		return Messages.format("", new String[]{getCategoryLabel(fRepetition)});
	}

	private String createIterationMessage() {
		return Messages.format("", new String[]{ getCategoryLabel(fRepetition), fIterationGesture, getCategoryLabel(fRepetition + 1) });
	}

	private String getCategoryLabel(int repetition) {
		int iteration= repetition % fCategoryIteration.size();
		if (iteration == 0)
			return "";
		return toString(fCategoryIteration.get(iteration).get(0));
	}

	private String toString(CompletionProposalCategory category) {
		return category.getDisplayName();
	}

	private String getIterationGesture() {
		TriggerSequence binding= getIterationBinding();
		return binding != null ?
				  Messages.format("", new Object[] { binding.format() })
				: "";
	}

	private KeySequence getIterationBinding() {
	    final IBindingService bindingSvc= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		TriggerSequence binding= bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		if (binding instanceof KeySequence)
			return (KeySequence) binding;
		return null;
    }

	/**
	 * Sets the current proposal sorter into the content assistant.
	 * 
	 * @since 3.8
	 * @see ProposalSorterRegistry#getCurrentSorter() the sorter used if <code>true</code>
	 */
	private void setContentAssistSorter() {
		ProposalSorterHandle currentSorter= ProposalSorterRegistry.getDefault().getCurrentSorter();
//		try {
//			fAssistant.setSorter(currentSorter.getSorter());
//		} catch (InvalidRegistryObjectException x) {
//			//JavaPlugin.log(currentSorter.createExceptionStatus(x));
//		} catch (CoreException x) {
//			//JavaPlugin.log(currentSorter.createExceptionStatus(x));
//		} catch (RuntimeException x) {
//			//JavaPlugin.log(currentSorter.createExceptionStatus(x));
//		}
	}
}
