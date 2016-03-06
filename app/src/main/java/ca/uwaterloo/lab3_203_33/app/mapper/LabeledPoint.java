package ca.uwaterloo.lab3_203_33.app.mapper;

import android.graphics.PointF;

/**
 * A class that associates a point and a label.
 * @author Kirill
 *
 */
public class LabeledPoint {
	PointF point;
	String label;
	
	LabeledPoint(PointF point, String label)
	{
		this.point = new PointF(point.x, point.y);
		this.label = label;
	}
}
