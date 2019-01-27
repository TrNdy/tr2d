package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.indago.tr2d.ui.model.Tr2dSegmentationEditorModel;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labkit.LabelingComponent;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.labkit.models.ImageLabelingModel;
import net.imglib2.type.numeric.NumericType;
import net.miginfocom.swing.MigLayout;

public class Tr2dSegmentationEditorPanel extends JPanel {

	private final Tr2dSegmentationEditorModel model;

	private final JButton reloadButton = initReloadButton();

	private final JCheckBox checkBox = initCheckBox();

	private JComponent editorComponent;

	public Tr2dSegmentationEditorPanel(
			final Tr2dSegmentationEditorModel segmentationModel) {
		this.model = segmentationModel;
		setLayout(new BorderLayout());
		add(initTopPanel(), BorderLayout.PAGE_START);
		if(model.useManualSegmentation())
			checkBox.setSelected(true);
	}

	private JButton initReloadButton() {
		final JButton button = new JButton("refresh");
		button.setVisible(false);
		button.addActionListener(ignore -> fetch());
		return button;
	}

	private void fetch() {
		model.fetch();
		addEditor();
	}

	private JCheckBox initCheckBox() {
		final JCheckBox checkBox = new JCheckBox("edit segments manually");
		checkBox.addItemListener( ignore -> checkBoxClicked() );
		return checkBox;
	}

	private JComponent initTopPanel() {
		final JPanel top = new JPanel();
		top.setLayout(new MigLayout());
		initCheckBox();
		top.add(checkBox);
		top.add(reloadButton);
		return top;
	}

	private void checkBoxClicked() {
		final boolean selected = checkBox.isSelected();
		model.setUseManualSegmentation(selected);
		reloadButton.setVisible(selected);
		if(selected)
			addEditor();
		else
			removeEditor();
	}

	private void removeEditor() {
		if(editorComponent == null)
			return;
		remove(editorComponent);
		repaint();
		editorComponent = null; // remove reference to enable garbage collection
	}

	private void addEditor() {
		removeEditor();
		final RandomAccessibleInterval< ? extends NumericType< ? > > image = model.getModel().getRawData();
		try {
			final ImageLabelingModel labelingComponentModel =
					new ImageLabelingModel(image, Labeling.fromImgLabeling(model.asLabeling()), true);
			editorComponent = new LabelingComponent(null, labelingComponentModel)
					.getComponent();
		} catch (final NoClassDefFoundError e) {
			editorComponent = new JLabel("Please install Labkit to enable manual segmentation.");
		}
		add(editorComponent);
		repaint();
	}

}
