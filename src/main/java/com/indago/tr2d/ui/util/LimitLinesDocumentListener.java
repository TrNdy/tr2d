package com.indago.tr2d.ui.util;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.jfree.util.Log;

/*
 *  A class to control the maximum number of lines to be stored in a Document
 *
 *  Excess lines can be removed from the start or end of the Document
 *  depending on your requirement.
 *
 *  a) if you append text to the Document, then you would want to remove lines
 *     from the start.
 *  b) if you insert text at the beginning of the Document, then you would
 *     want to remove lines from the end.
 */
public class LimitLinesDocumentListener implements DocumentListener
{
	private int maximumLines;
	private final boolean isRemoveFromStart;

	/*
	 *  Specify the number of lines to be stored in the Document.
	 *  Extra lines will be removed from the start of the Document.
	 */
	public LimitLinesDocumentListener(final int maximumLines)
	{
		this(maximumLines, true);
	}

	/*
	 *  Specify the number of lines to be stored in the Document.
	 *  Extra lines will be removed from the start or end of the Document,
	 *  depending on the boolean value specified.
	 */
	public LimitLinesDocumentListener(final int maximumLines, final boolean isRemoveFromStart)
	{
		setLimitLines(maximumLines);
		this.isRemoveFromStart = isRemoveFromStart;
	}

	/*
	 *  Return the maximum number of lines to be stored in the Document
	 */
	public int getLimitLines()
	{
		return maximumLines;
	}

	/*
	 *  Set the maximum number of lines to be stored in the Document
	 */
	public void setLimitLines(final int maximumLines)
	{
		if (maximumLines < 1)
		{
			final String message = "Maximum lines must be greater than 0";
			throw new IllegalArgumentException(message);
		}

		this.maximumLines = maximumLines;
	}

	//  Handle insertion of new text into the Document

	@Override
	public void insertUpdate(final DocumentEvent e)
	{
		//  Changes to the Document can not be done within the listener
		//  so we need to add the processing to the end of the EDT

		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				removeLines(e);
			}
		});
	}

	@Override
	public void removeUpdate(final DocumentEvent e) {}
	@Override
	public void changedUpdate(final DocumentEvent e) {}

	/*
	 *  Remove lines from the Document when necessary
	 */
	private void removeLines(final DocumentEvent e)
	{
		//  The root Element of the Document will tell us the total number
		//  of line in the Document.

		final Document document = e.getDocument();
		final Element root = document.getDefaultRootElement();

		while (root.getElementCount() > maximumLines)
		{
			if (isRemoveFromStart)
			{
				removeFromStart(document, root);
			}
			else
			{
				removeFromEnd(document, root);
			}
		}
	}

	/*
	 *  Remove lines from the start of the Document
	 */
	private void removeFromStart(final Document document, final Element root)
	{
		final Element line = root.getElement(0);
		final int end = line.getEndOffset();

		try
		{
			document.remove(0, end);
		}
		catch(final BadLocationException ble)
		{
			Log.error( ble );
		}
	}

	/*
	 *  Remove lines from the end of the Document
	 */
	private void removeFromEnd(final Document document, final Element root)
	{
		//  We use start minus 1 to make sure we remove the newline
		//  character of the previous line

		final Element line = root.getElement(root.getElementCount() - 1);
		final int start = line.getStartOffset();
		final int end = line.getEndOffset();

		try
		{
			document.remove(start - 1, end - start);
		}
		catch(final BadLocationException ble)
		{
			Log.error( ble );
		}
	}
}