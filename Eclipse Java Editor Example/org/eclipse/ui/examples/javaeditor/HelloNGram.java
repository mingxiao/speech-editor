/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package org.eclipse.ui.examples.javaeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;


/**
 * A simple HelloNGram demo showing a simple speech application built using Sphinx-4. This application uses the Sphinx-4
 * endpointer, which automatically segments incoming audio into utterances and silences.
 */
public class HelloNGram {
	ConfigurationManager cm;
	Recognizer recognizer;
//	Text text;

	public HelloNGram(){
//		text = t;
		System.out.println("Inside Hellongram=======");

        cm = new ConfigurationManager(HelloNGram.class.getResource("hellongram.config.xml"));

        // allocate the recognizer
        System.out.println("Loading...");
        recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();

        // start the microphone or exit if the programm if this is not possible
        Microphone microphone = (Microphone) cm.lookup("microphone");
        if (!microphone.startRecording()) {
            System.out.println("Cannot start microphone.");
            recognizer.deallocate();
            System.exit(1);
        }
//
//        printInstructions();
//
        // loop the recognition until the programm exits.
//        try {
//			record();
//		} catch (BadLocationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public String[] record2(){
		System.out.println("Start speaking...");
		Result result = recognizer.recognize();
		while(result == null){
			System.out.println("recognizing again...");
			result = recognizer.recognize();
		}
		String resultText = result.getBestResultNoFiller();
		List<String> possibleWords = new ArrayList<String>();
		List<Token> tokens = result.getResultTokens();
		for(Token t : tokens){
//			System.out.println("adding tokens");
//			System.out.println("word path: "+t.getWordPath()); //this is the information that we want!
			possibleWords.add(t.getWordPath());
//			System.out.println("word: "+t.getWord().getSpelling());
//			System.out.println("To String: "+t.toString());
		}
        System.out.println("You said: " + resultText + '\n');
//        try {
//			text.insertText(resultText);
//		} catch (BadLocationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        String[] res = possibleWords.toArray(new String[0]);
//        for (String r : res)
//        	System.out.println(r);
        return res;
	}
	
	public String record() throws BadLocationException{
		while (true) {
            System.out.println("Start speaking. Press Ctrl-C to quit.\n");

            Result result = recognizer.recognize();

            if (result != null) {
                String resultText = result.getBestResultNoFiller();
                System.out.println("You said: " + resultText + '\n');
//                text.insertText(resultText);
                return resultText;
            } else {
                System.out.println("I can't hear what you said.\n");
                return null;
            }
        }
	}
	
    public static void main(String[] args) {
        ConfigurationManager cm;

        if (args.length > 0) {
            cm = new ConfigurationManager(args[0]);
        } else {
            cm = new ConfigurationManager(HelloNGram.class.getResource("hellongram.config.xml"));
        }

        // allocate the recognizer
        System.out.println("Loading...");
        Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();

        // start the microphone or exit if the programm if this is not possible
        Microphone microphone = (Microphone) cm.lookup("microphone");
        if (!microphone.startRecording()) {
            System.out.println("Cannot start microphone.");
            recognizer.deallocate();
            System.exit(1);
        }

        printInstructions();

        // loop the recognition until the programm exits.
        while (true) {
            System.out.println("Start speaking. Press Ctrl-C to quit.\n");

            Result result = recognizer.recognize();

            if (result != null) {
                String resultText = result.getBestResultNoFiller();
                System.out.println("You said: " + resultText + '\n');
            } else {
                System.out.println("I can't hear what you said.\n");
            }
        }
    }


    /** Prints out what to say for this demo. */
    private static void printInstructions() {
        System.out.println("Sample sentences:\n" +
                "the green one right in the middle\n" +
                "the purple one on the lower right side\n" +
                "the closest purple one on the far left side\n" +
                "the only one left on the left\n\n" +
                "Refer to the file hellongram.test for a complete list.\n");
    }
}
