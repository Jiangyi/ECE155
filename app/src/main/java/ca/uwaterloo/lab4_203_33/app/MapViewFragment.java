package ca.uwaterloo.lab4_203_33.app;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import ca.uwaterloo.lab4_203_33.app.mapper.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jycyq on 2016-03-06.
 */
public class MapViewFragment extends Fragment {

    // Member variable for the actual map view that the fragment will be containing
    private MapView mapView;
    private OrientationManager orientationManager;
    private StepCounterManager stepCounterManager;

    private static boolean pathSet = false;
    private int angle = 0;
    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    };
    private Direction calcDirection;
    private static final float USER_POINT_OFFSET = 0.1f;
    private static final float CHECK_WALL_OFFSET = 0.2f;
    private static final float STEP_CONSTANT = 0.75f;
    private ImageView compassView, waypointView;
    /**
     * Returns a new instance of this fragment.
     */
    public static MapViewFragment newInstance(Context context) {
        MapViewFragment fragment = new MapViewFragment();
        return fragment;
    }

    public MapViewFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_mapview, container, false);
        compassView = (ImageView) rootView.findViewById(R.id.compass_arrow);
        waypointView = (ImageView) rootView.findViewById(R.id.waypoint_arrow);
        // Instantiate the map view (Fits a 1280x720 panel at this moment)
        mapView = new MapView(rootView.getContext(), 700, 1000, 25, 25);
        // Get map object from the MapLoader
        PedometerMap map = MapLoader.loadMap(new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "E2-3344.svg"));
        // Give the map to the MapView to display
        mapView.setMap(map);
        // Add the MapView to the fragment layout
        RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.mapview_layout);
        layout.addView(mapView, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {{
            addRule(RelativeLayout.BELOW, compassView.getId());
        }});

        // Call the step counter manager and register a listener on it
        stepCounterManager = StepCounterManager.getInstance();
        stepCounterManager.registerListener(new StepCounterManager.StepCounterListener() {
            @Override
            public void onStepChanged(int stepCounter) {
                PointF userPoint = mapView.getUserPoint();
                PointF newPoint = new PointF((float) (userPoint.x - STEP_CONSTANT * Math.sin(Math.toRadians(angle))),
                                             (float) (userPoint.y - STEP_CONSTANT * Math.cos(Math.toRadians(angle))));
                List<InterceptPoint> list = mapView.calculateIntersections(userPoint, newPoint);
                if (list.isEmpty()) {
                    calculateUserPath(newPoint, mapView.getEndPoint());
                } else {
                    PointF intercept = list.get(0).getPoint();
                    intercept.x += USER_POINT_OFFSET * Math.sin(Math.toRadians(angle));
                    intercept.y += USER_POINT_OFFSET * Math.cos(Math.toRadians(angle));
                    calculateUserPath(intercept, mapView.getEndPoint());
                }
            }

            @Override
            public void onDataPointAdded(float dataPoint) {
                // Do nothing
            }
        });

        // Call the orientation manager and register a listener on it
        orientationManager = OrientationManager.getInstance();
        orientationManager.registerListener(new OrientationManager.OrientationListener() {
            @Override
            public void onOrientationChanged(float azimuth) {
                // When the orientation has changed, convert and normalize the direction to
                // degrees between 0 and 360
                angle = (int) ((Math.round(Math.toDegrees(azimuth)) + 360) % 360);
                compassView.setRotation(-angle);
            }
        });
        // Register the MapView context menu
        registerForContextMenu(mapView);

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        // Inflate the MapView context menu and add options
        getActivity().getMenuInflater().inflate(R.menu.mapview_menu, menu);
        mapView.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean ret = mapView.onContextItemSelected(item);
        PointF startPoint = mapView.getStartPoint();
        PointF endPoint = mapView.getEndPoint();
        if (startPoint.x != 0 && startPoint.y != 0
                && endPoint.x != 0 && endPoint.y != 0) {
            calculateUserPath(startPoint, endPoint);
        }
        return ret;
    }

    private void updateWaypointCompass(ArrayList<PointF> userPath) {
        PointF startPoint = userPath.get(0);
        PointF destPoint;
        if (isCloseEnough(userPath.get(0), userPath.get(1))) {
            if (userPath.size() > 2) {
                destPoint = userPath.get(2);
            } else {
                Toast.makeText(getContext(), getString(R.string.destination_arrival), Toast.LENGTH_SHORT).show();
                pathSet = false;
                return;
            }
        } else {
            destPoint = userPath.get(1);
        }
        double y = startPoint.y - destPoint.y;
        double x = destPoint.x - startPoint.x;
        int rotAngle = (int) Math.round((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
        waypointView.setRotation(-rotAngle);

    }

    private boolean isCloseEnough(PointF a, PointF b) {
        return Math.abs(a.x - b.x) < 0.5 && Math.abs(a.y - b.y) < 0.5;
    }
    private void calculateUserPath(PointF start, PointF end) {
        List<InterceptPoint> intersectList = mapView.calculateIntersections(start, end);
        ArrayList<PointF> userPath = new ArrayList<PointF>();
        if (!intersectList.isEmpty()) {
            userPath.add(new PointF(start.x, start.y));
            PointF point;
            if (start.x < end.x) {
                point = mapView.calculateIntersections(start, new PointF(start.x + 1000, start.y)).get(0).getPoint();
                point.x -= USER_POINT_OFFSET;
                calcDirection = Direction.SOUTH;
            } else {
                point = mapView.calculateIntersections(start, new PointF(start.x - 1000, start.y)).get(0).getPoint();
                point.x += USER_POINT_OFFSET;
                calcDirection = Direction.NORTH;
            }
//            if (Math.abs(intercept.getLine().start.x - intercept.getLine().end.x) < 0.01) {
//                // Vertical wall
//                calcDirection = start.x < end.x ? Direction.SOUTH
//                        : Direction.NORTH;
//                x = start.x < end.x ? intercept.getPoint().x - USER_POINT_OFFSET
//                        : intercept.getPoint().x + USER_POINT_OFFSET;
//                y = intercept.getPoint().y;
//            } else {
//                // Horizontal wall
//                calcDirection = start.y < end.y ? Direction.EAST
//                        : Direction.WEST;
//                x = intercept.getPoint().x;
//                y = start.y < end.y ? intercept.getPoint().y + USER_POINT_OFFSET
//                        : intercept.getPoint().y - USER_POINT_OFFSET;
//            }
            userPath.add(point);

            while (!mapView.calculateIntersections(userPath.get(userPath.size() - 1), end).isEmpty()) {
                PointF userPoint = userPath.get(userPath.size() - 1);
                LineSegment wall = getLeftSideWall(userPoint, calcDirection);
                PointF endPoint = getWantedEndPoint(wall);

                userPath.add(getNextUserPoint(userPoint, endPoint));
            }
            userPath.add(end);
        } else {
            userPath.add(start);
            userPath.add(end);
        }
        pathSet = true;
        mapView.setUserPath(userPath);
        mapView.setUserPoint(start);
        updateWaypointCompass(userPath);
    }

    private LineSegment getLeftSideWall(PointF currentPoint, Direction direction) {

        PointF currentModified, compare;
        switch (direction) {
            case NORTH:
                currentModified = new PointF(currentPoint.x, currentPoint.y - CHECK_WALL_OFFSET);
                compare = new PointF(currentModified.x - CHECK_WALL_OFFSET, currentModified.y);
                break;
            case SOUTH:
                currentModified = new PointF(currentPoint.x, currentPoint.y + CHECK_WALL_OFFSET);
                compare = new PointF(currentModified.x + CHECK_WALL_OFFSET, currentModified.y);
                break;
            case EAST:
                currentModified = new PointF(currentPoint.x + CHECK_WALL_OFFSET, currentPoint.y);
                compare = new PointF(currentModified.x, currentModified.y - CHECK_WALL_OFFSET);
                break;
            case WEST:
                currentModified = new PointF(currentPoint.x - CHECK_WALL_OFFSET, currentPoint.y);
                compare = new PointF(currentModified.x, currentModified.y + CHECK_WALL_OFFSET);
                break;
            default:
                // Will never get here
                Log.e("getLeftSideWall", "Something went wrong, this should not be seen!!!");
                currentModified = new PointF();
                compare = new PointF();
                break;
        }
        List<InterceptPoint> list = mapView.calculateIntersections(currentModified, compare);
        if (!list.isEmpty()) {
            return list.get(0).getLine();
        } else {
            return null;
        }
    }

    private PointF getWantedEndPoint(LineSegment wall) {
        float x = 0, y = 0;
        switch (calcDirection) {
            case NORTH:
                if (wall.start.y < wall.end.y) {
                    return wall.start;
                } else {
                    return wall.end;
                }
            case SOUTH:
                if (wall.start.y > wall.end.y) {
                    return wall.start;
                } else {
                    return wall.end;
                }
            case EAST:
                if (wall.start.x > wall.end.x) {
                    return wall.start;
                } else {
                    return wall.end;
                }
            case WEST:
                if (wall.start.x < wall.end.x) {
                    return wall.start;
                } else {
                    return wall.end;
                }
        }
        return null;
    }

    private PointF getNextUserPoint(PointF currentPoint, PointF endPoint) {
        PointF out;
        List<InterceptPoint> list;
        switch (calcDirection) {
            case NORTH:
                out = new PointF(currentPoint.x, endPoint.y - USER_POINT_OFFSET);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.y += USER_POINT_OFFSET;
                    calcDirection = Direction.EAST;
                } else {
                    calcDirection = Direction.WEST;
                }
                return out;
            case SOUTH:
                out = new PointF(currentPoint.x, endPoint.y + USER_POINT_OFFSET);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.y -= USER_POINT_OFFSET;
                    calcDirection = Direction.WEST;
                } else {
                    calcDirection = Direction.EAST;
                }
                return out;
            case EAST:
                out = new PointF(endPoint.x + USER_POINT_OFFSET, currentPoint.y);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.x -= USER_POINT_OFFSET;
                    calcDirection = Direction.SOUTH;
                } else {
                    calcDirection = Direction.NORTH;
                }
                return out;
            case WEST:
                out = new PointF(endPoint.x - USER_POINT_OFFSET, currentPoint.y);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.x += USER_POINT_OFFSET;
                    calcDirection = Direction.NORTH;
                } else {
                    calcDirection = Direction.SOUTH;
                }
                return out;
        }
        // Shouldn't reach here
        return null;
    }

    public static boolean isPathSet() {
        return pathSet;
    }
}
