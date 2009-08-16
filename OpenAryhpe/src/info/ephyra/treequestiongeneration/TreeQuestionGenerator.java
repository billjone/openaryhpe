package info.ephyra.treequestiongeneration;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;

import edu.stanford.nlp.trees.Tree;

import info.ephyra.io.MsgPrinter;
import info.ephyra.nlp.OpenNLP;
import info.ephyra.nlp.TreeUtil;
import info.ephyra.questionanalysis.Term;
import info.ephyra.treeansweranalysis.TreeAnswer;

public class TreeQuestionGenerator {
	
	// generate questions according to qaPhrasePair in treeAnswer
	public static void generate(TreeAnswer treeAnswer) {
		ArrayList<QAPhrasePair> qaPhraseList = treeAnswer.getQAPhraseList();
		QAPhrasePair pPair;
		Tree ansTree, auxTree, invTree, sentTree;
		Term ansTerm;
		String ansPhrase, quesPhrase, quesSent, auxSent, invSent, subject, oriSent, idxSent;
		auxTree = treeAnswer.getAuxTree();
		invTree = treeAnswer.getInvTree();
		auxSent = treeAnswer.getAuxSentence();
		invSent = treeAnswer.getInvSentence();
		subject = treeAnswer.getSubject();
		oriSent = treeAnswer.getSentence();
		sentTree = treeAnswer.getTree();
		idxSent = TreeUtil.getLabel(sentTree);
		Iterator<QAPhrasePair> iter = qaPhraseList.iterator();
		while (iter.hasNext()) {
			pPair = iter.next();
			ansTree = pPair.getAnsTree();
			ansTerm = pPair.getAnsTerm();
			//ansPhrase = OpenNLP.tokenizeWithSpaces(pPair.getAnsPhrase());
			ansPhrase = pPair.getAnsPhrase();
			quesPhrase = pPair.getQuesPhrase();
			if (pPair.getQuesType().equals("Y/N")) {
				quesSent = invSent.replaceFirst("<quesPhrase>", "");;
			} else {
				// here we use "contains", not "equals" to include the following case:
				// In "The New York Times wrote that.", the subject is "Times-4",
				// but the answer phrase is "The-1 New-2 York-3 Times-4".
				if (subject != null && ansPhrase.contains(subject)) {
					// answer phrase is the subject
					quesSent = idxSent.replaceFirst(ansPhrase, quesPhrase);
				} else {
					quesSent = invSent.replaceFirst(OpenNLP.tokenizeWithSpaces(ansPhrase), "");
					quesSent = quesSent.replaceFirst("<quesPhrase>", quesPhrase);
				}
			}
			// remove the index
			quesSent = quesSent.replaceAll("-\\d+\\b", "");
			// recover ()
			quesSent = quesSent.replaceAll("-LRB-", "(");
			quesSent = quesSent.replaceAll("-RRB-", ")");
			// remove the punctuation at the end and append with a question mark
			quesSent = quesSent.replaceAll("(\\.|\\?|!)$", "").trim()+"?";
			// remove extra spaces
			quesSent = quesSent.replaceAll("\\s{2,}", " ");
			// Capitalize the first letter
			quesSent = quesSent.substring(0,1).toUpperCase() + quesSent.substring(1);
			pPair.setQuesSentence(quesSent);
			// generate another y/n question here, which should be invSent with the first capitalized
			//TODO: post-processing here
			//TODO: parse quesSentence and judge whether it's grammatical according to the score
		}
		return;
	}
	
	public static void print(ArrayList<TreeAnswer> treeAnswerList) {
		Iterator<TreeAnswer> tAnsIter = treeAnswerList.iterator();
		TreeAnswer treeAnswer;
		ArrayList<QAPhrasePair> qaPhraseList;
		Iterator<QAPhrasePair> pPairIter;
		QAPhrasePair pPair;
		int sentCount=0, quesCount=0;
		while (tAnsIter.hasNext()) {
			sentCount++;
			quesCount=0;
			treeAnswer = tAnsIter.next();
			MsgPrinter.printStatusMsg(sentCount+". "+treeAnswer.getSentence());
			qaPhraseList = treeAnswer.getQAPhraseList();
			pPairIter = qaPhraseList.iterator();
			while (pPairIter.hasNext()) {
				quesCount++;
				pPair = pPairIter.next();
				MsgPrinter.printStatusMsg("\t"+quesCount+". Q type: "+pPair.getQuesType()+" NE type: "+pPair.getAnsTerm());
				MsgPrinter.printStatusMsg("\t\tQuestion: "+pPair.getQuesSentence());
				MsgPrinter.printStatusMsg("\t\tAnswer: "+pPair.getAnsPhrase().replaceAll("-\\d+\\b", ""));
			}
			
		}
	}
	
	public static void printForEvaluation(ArrayList<TreeAnswer> treeAnswerList, BufferedWriter out) {
		Iterator<TreeAnswer> tAnsIter = treeAnswerList.iterator();
		TreeAnswer treeAnswer;
		ArrayList<QAPhrasePair> qaPhraseList;
		Iterator<QAPhrasePair> pPairIter;
		QAPhrasePair pPair;
		int sentCount=0, quesCount=0;
		try {
			while (tAnsIter.hasNext()) {
				sentCount++;
				quesCount=0;
				treeAnswer = tAnsIter.next();
				out.write(sentCount+". "+treeAnswer.getSentence());
				out.newLine();
				qaPhraseList = treeAnswer.getQAPhraseList();
				pPairIter = qaPhraseList.iterator();
				while (pPairIter.hasNext()) {
					quesCount++;
					pPair = pPairIter.next();
					out.write("\t"+quesCount+". Question: "+pPair.getQuesSentence());
					out.newLine();
					out.write("\t\tPossible answer: "+pPair.getAnsPhrase().replaceAll("-\\d+\\b", ""));
					out.newLine();
					out.write("\t\tYour judgement: [Acceptable] [Ungram] [No sense] [Vague] [Obvious] [Missing] [Wrong WH] [Format] Other:");
					out.newLine();
				}
				out.newLine();
				
			}
		} catch (java.io.IOException e) {
			System.err.println(e);
		}
	}
}
