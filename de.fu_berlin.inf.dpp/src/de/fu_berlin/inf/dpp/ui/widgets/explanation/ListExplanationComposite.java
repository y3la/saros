package de.fu_berlin.inf.dpp.ui.widgets.explanation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

/**
 * This composite displays a simple {@link ExplanationComposite} and allows it's
 * content to be scrollable if the composite becomes to small.
 * <p>
 * This composite does <strong>NOT</strong> handle setting the layout and adding
 * sub {@link Control}s correctly.
 * 
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>NONE and those supported by {@link ExplanationComposite}</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * 
 * @see ExplanationComposite
 * @author bkahlert
 * 
 */
public class ListExplanationComposite extends ExplanationComposite {
    /**
     * Instances of this class are used to set the contents of an
     * {@link ListExplanationComposite} instance.
     * 
     * @see ListExplanationComposite#setExplanation(ListExplanation)
     */
    public static class ListExplanation {
        protected String introductoryText;
        protected String[] listItems;
        protected Image explanationImage;

        /**
         * Constructs a new explanation for use with
         * {@link ListExplanationComposite}.
         * 
         * @param introductoryText
         *            introduces the list items
         * @param listItems
         *            describes the list items
         */
        public ListExplanation(String introductoryText, String... listItems) {
            this(null, introductoryText, listItems);
        }

        /**
         * Constructs a new explanation for use with
         * {@link ListExplanationComposite}.
         * 
         * @param systemImage
         *            SWT constant that declares a system image (e.g.
         *            {@link SWT#ICON_INFORMATION})
         * @param introductoryText
         *            introduces the list items
         * @param listItems
         *            describes the list items
         */
        public ListExplanation(int systemImage, String introductoryText,
            String... listItems) {
            this(Display.getDefault().getSystemImage(systemImage),
                introductoryText, listItems);
        }

        /**
         * Constructs a new explanation for use with
         * {@link ListExplanationComposite}.
         * 
         * @param explanationImage
         *            Explanatory image {@link SWT#ICON_INFORMATION})
         * @param introductoryText
         *            introduces the list items
         * @param listItems
         *            describes the list items
         */
        public ListExplanation(Image explanationImage, String introductoryText,
            String... listItems) {
            this.introductoryText = introductoryText;
            this.listItems = listItems;
            this.explanationImage = explanationImage;
        }
    }

    /**
     * Instances of this class layout text in the form of an intro text and list
     * items.
     * 
     * @see ListExplanationComposite
     */
    protected static class ListExplanationContentComposite extends Composite {
        /* number of columns used for layout */
        final int numCols = 2;

        public ListExplanationContentComposite(Composite parent, int style,
            ListExplanation listExplanation) {
            super(parent, style);

            this.setLayout(new GridLayout(numCols, false));

            /*
             * Introductory text
             */
            if (listExplanation.introductoryText != null) {
                Label introductoryLabel = new Label(this, SWT.WRAP);
                introductoryLabel.setLayoutData(new GridData(SWT.FILL,
                    SWT.FILL, true, true, numCols, 1));
                introductoryLabel.setText(listExplanation.introductoryText);
            }

            /*
             * List items
             */
            if (listExplanation.listItems != null) {
                for (int i = 0; i < listExplanation.listItems.length; i++) {
                    Label stepNumber = new Label(this, SWT.NONE);
                    stepNumber.setLayoutData(new GridData(SWT.BEGINNING,
                        SWT.BEGINNING, false, true));
                    stepNumber.setText((i + 1) + ")");

                    Label stepContent = new Label(this, SWT.WRAP);
                    stepContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
                        true, true, 1, 1));
                    stepContent.setText(listExplanation.listItems[i]);
                }
            }
        }
    }

    /**
     * This composite holds the contents
     */
    protected ListExplanationContentComposite contentComposite;

    /**
     * Constructs a new explanation composite.
     * 
     * @param parent
     *            The parent control
     * @param style
     *            Style constants
     */
    public ListExplanationComposite(Composite parent, int style) {
        super(parent, style, null);

        super.setLayout(new GridLayout(1, false));
    }

    /**
     * Constructs a new explanation composite.
     * 
     * @param parent
     *            The parent control
     * @param style
     *            Style constants
     * @param listExplanation
     *            Explanation to be displayed by the
     *            {@link SimpleExplanationComposite}
     */
    public ListExplanationComposite(Composite parent, int style,
        ListExplanation listExplanation) {
        this(parent, style);
        setExplanation(listExplanation);
    }

    /**
     * Sets the explanation
     * 
     * @param listExplanation
     *            Explanation to be displayed by the
     *            {@link SimpleExplanationComposite}
     */
    public void setExplanation(ListExplanation listExplanation) {

        this.setExplanationImage((listExplanation != null) ? listExplanation.explanationImage
            : null);

        if (this.contentComposite != null
            && !this.contentComposite.isDisposed())
            this.contentComposite.dispose();
        this.contentComposite = new ListExplanationContentComposite(this,
            SWT.NONE, listExplanation);
        this.contentComposite.setLayoutData(new GridData(SWT.BEGINNING,
            SWT.CENTER, true, true));
        this.layout();
    }

    @Override
    public void setLayout(Layout layout) {
        // this composite controls its layout itself
    }
}