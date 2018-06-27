package com.indago.tr2d.ui.view;

import com.indago.tr2d.io.projectfolder.Tr2dProjectFolder;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.model.Tr2dSegmentationEditorModel;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labkit.LabelingComponent;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.labkit.models.ImageLabelingModel;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.IntType;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Tr2dSegmentationEditorPanel extends JPanel {

	private final Tr2dSegmentationEditorModel model;

	private final JButton reloadButton = initReloadButton();

	private final JCheckBox checkBox = initCheckBox();

	private JComponent editorComponent;

	public Tr2dSegmentationEditorPanel(
			Tr2dSegmentationEditorModel segmentationModel) {
		this.model = segmentationModel;
		setLayout(new BorderLayout());
		add(initTopPanel(), BorderLayout.PAGE_START);
		if(model.useManualSegmentation())
			checkBox.setSelected(true);
	}

	private JButton initReloadButton() {
		JButton button = new JButton("refresh");
		button.setVisible(false);
		button.addActionListener(ignore -> fetch());
		return button;
	}

	private void fetch() {
		model.fetch();
		addEditor();
	}

	private JCheckBox initCheckBox() {
		JCheckBox checkBox = new JCheckBox("edit segments manually");
		checkBox.addItemListener( ignore -> checkBoxClicked() );
		return checkBox;
	}

	private JComponent initTopPanel() {
		JPanel top = new JPanel();
		top.setLayout(new MigLayout());
		initCheckBox();
		top.add(checkBox);
		top.add(reloadButton);
		return top;
	}

	private void checkBoxClicked() {
		boolean selected = checkBox.isSelected();
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
		RandomAccessibleInterval< ? extends NumericType< ? > > image = model.getModel().getRawData();
		ImageLabelingModel labelingComponentModel =
				new ImageLabelingModel(image, new Labeling(model.asLabeling()), true);
		editorComponent = new LabelingComponent(null, labelingComponentModel)
				.getComponent();
		add(editorComponent);
		repaint();
	}

	public static void main(String... args) throws IOException {
		// demo
		JFrame frame = new JFrame();
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Path tmpFolder = Files.createTempDirectory(null);
		Tr2dProjectFolder projectFolder =
				new Tr2dProjectFolder(tmpFolder.toFile());
		ImagePlus image = NewImage.createFloatImage("title", 200, 200, 10, 0);
		IJ.save(image, projectFolder + "/raw.tif");
		projectFolder.initialize();
		Tr2dModel model = new Tr2dModel(projectFolder, image);
		Tr2dSegmentationEditorModel editorModel = model.getSegmentationEditorModel();
		frame.add( new Tr2dSegmentationEditorPanel(editorModel) );
		frame.setVisible(true);
	}

}
