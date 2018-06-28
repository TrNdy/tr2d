package com.indago.tr2d.ui.view;

import com.indago.tr2d.ui.model.Tr2dSegmentationEditorModel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labkit.LabelingComponent;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.labkit.models.ImageLabelingModel;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.IntType;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Tr2dSegmentationEditorPanel extends JPanel {

	private final JTabbedPane tabs = new JTabbedPane();

	private final Tr2dSegmentationEditorModel model;

	public Tr2dSegmentationEditorPanel(
			Tr2dSegmentationEditorModel segmentationModel) {
		this.model = segmentationModel;
		setLayout(new BorderLayout());
		add(loadButton(), BorderLayout.PAGE_START);
		add(tabs);
	}

	private JButton loadButton() {
		JButton button = new JButton("load");
		button.addActionListener(ignore -> addEditor());
		return button;
	}

	private void addEditor() {
		RandomAccessibleInterval< ? extends NumericType< ? > > image = model.getModel().getRawData();
		ImageLabelingModel labelingComponentModel =
				new ImageLabelingModel(image, new Labeling(model.asLabeling()), true);
		tabs.addTab("Segmentation",
				new LabelingComponent(null, labelingComponentModel)
						.getComponent());
	}

}
