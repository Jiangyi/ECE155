package ca.uwaterloo.lab4_203_33.app;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
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

    // Constants for various calculations
    private static final float USER_POINT_OFFSET = 0.1f;
    private static final float CHECK_WALL_OFFSET = 0.2f;
    private static final float STEP_CONSTANT = 0.75f;
    // Member variable for the actual map view that the fragment will be containing
    private MapView mapView;
    // Member variables for the two managers we will be relying on
    private OrientationManager orientationManager;
    private StepCounterManager stepCounterManager;

    // Boolean for whether the path is set or not
    private static boolean pathSet = false;

    // The angle the compass is currently in
    private int compassAngle = 0;

    // Helper enum for calculating the user path
    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    };

    // The direction to take that comes next after the current point in the user path
    private Direction nextDirection;

    // ImageViews for updating the two arrows for compass
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
        // Get references to the two compass arrows
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
                // Get the current user point
                PointF userPoint = mapView.getUserPoint();
                // Calculate the next user point
                PointF newPoint = new PointF((float) (userPoint.x - STEP_CONSTANT * Math.sin(Math.toRadians(compassAngle))),
                        (float) (userPoint.y - STEP_CONSTANT * Math.cos(Math.toRadians(compassAngle))));
                // See if there is a wall between the current point and the next point
                List<InterceptPoint> list = mapView.calculateIntersections(userPoint, newPoint);
                if (list.isEmpty()) {
                    // If not, re-calculate the user path, with the next point set to the current user point
                    calculateUserPath(newPoint, mapView.getEndPoint());
                } else {
                    // There is a wall, so move to the closest point that's just off of the wall,
                    // and re-calculate the user path with that point as the current user point instead.
                    PointF intercept = list.get(0).getPoint();
                    intercept.x += USER_POINT_OFFSET * Math.sin(Math.toRadians(compassAngle));
                    intercept.y += USER_POINT_OFFSET * Math.cos(Math.toRadians(compassAngle));
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
                compassAngle = (int) ((Math.round(Math.toDegrees(azimuth)) + 360) % 360);
                compassView.setRotation(-compassAngle);
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
        // Get the start and end point of the map
        PointF startPoint = mapView.getStartPoint();
        PointF endPoint = mapView.getEndPoint();
        // Calculate a user path only if both points are set
        if (startPoint.x != 0 && startPoint.y != 0
                && endPoint.x != 0 && endPoint.y != 0) {
            calculateUserPath(startPoint, endPoint);
        }
        return ret;
    }

    // Updates the waypoint compass as need be
    private void updateWaypointCompass(ArrayList<PointF> userPath) {
        PointF startPoint = userPath.get(0);
        PointF destPoint;
        // Checks whether the next point is close enough to the current point
        if (isCloseEnough(userPath.get(0), userPath.get(1))) {
            // If it is, and there are more points, have the waypoint compass point to the next next point instead
            if (userPath.size() > 2) {
                destPoint = userPath.get(2);
            } else {
                // There are no more points. We are at our destination.
                Toast.makeText(getContext(), getString(R.string.destination_arrival), Toast.LENGTH_SHORT).show();
                pathSet = false;
                return;
            }
        } else {
            // Set waypoint compass to point to the next point
            destPoint = userPath.get(1);
        }
        // Calculate the angle between the points and rotate the waypoint compass image accordingly
        int rotAngle = calculateAngle(startPoint, destPoint);
        waypointView.setRotation(-rotAngle);

    }

    // Calculates the angle relative to East between two given points
    private int calculateAngle(PointF start, PointF end) {
        double y = start.y - end.y;
        double x = end.x - start.x;
        return (int) Math.round((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
    }

    // Checks whether two points are "close enough" to each other
    private boolean isCloseEnough(PointF a, PointF b) {
        return Math.abs(a.x - b.x) < 0.5 && Math.abs(a.y - b.y) < 0.5;
    }

    // Calculate the path the user should take to get to the destination
    // TODO: Make into calculating shortest path someday
    private void calculateUserPath(PointF start, PointF end) {
        // Calculate the intersection walls between the start and the end point
        List<InterceptPoint> intersectList = mapView.calculateIntersections(start, end);
        // Declare an arraylist to hold the individual points in the user path
        ArrayList<PointF> userPath = new ArrayList<PointF>();

        // Check if there are any walls between the start and the end
        if (!intersectList.isEmpty()) {
            // If there are walls, set the first point in the user path to be the start point
            userPath.add(new PointF(start.x, start.y));
            // Get the first intersection, and calculate the angle between it and the start point (Relative to East)
            PointF intersect = intersectList.get(0).getPoint();
            int angle = calculateAngle(start, intersect);

            // Calculate a point that is just off of it; This will be the point right after the start point
            PointF point = new PointF((float) (intersect.x - USER_POINT_OFFSET * Math.cos(Math.toRadians(angle))),
                    (float) (intersect.y + USER_POINT_OFFSET * Math.sin(Math.toRadians(angle))));

            // Check if the first intersecting line is vertical
            if (isLineVertical(intersectList.get(0).getLine())) {
                // If vertical, then based on the position of the start and end points, we will either go north or south
                nextDirection = start.x < end.x ? Direction.SOUTH : Direction.NORTH;
            } else {
                // If not, then based on the angle, we will determine whether to go east or west
                if (180 < angle && angle < 360) {
                    nextDirection = Direction.WEST;
                } else {
                    nextDirection = Direction.EAST;
                }
            }
            // Add the modified point to the user path
            userPath.add(point);

            // While loop to keep calculating points until we have a direct line of sight to the end point
            while (!mapView.calculateIntersections(userPath.get(userPath.size() - 1), end).isEmpty()) {
                PointF userPoint = userPath.get(userPath.size() - 1);
                // Get the wall that's on the left hand side
                LineSegment wall = getLeftSideWall(userPoint, nextDirection);
                if (wall != null) {
                    // Get the correct end point of the wall that we want
                    PointF endPoint = getWantedEndPoint(wall);
                    // Add the next point to the path list, and rinse and repeat
                    userPath.add(getNextUserPoint(userPoint, endPoint));
                } else {
                    // Extremely weird edge case that's nearly impossible to reproduce; Bail out
                    return;
                }
            }
            // Add the last point as the end point
            userPath.add(end);
        } else {
            // Clear line of sight; Path consists of just start and end
            userPath.add(start);
            userPath.add(end);
        }
        // Set the user path on the mapview with the constructed arraylist
        mapView.setUserPath(userPath);
        // Set the user to start at the start point
        mapView.setUserPoint(start);
        // Update the waypoint compass
        updateWaypointCompass(userPath);
        // The path is now created, so the boolean is set to true
        pathSet = true;
    }

    // Check if the given line segment is vertical
    private boolean isLineVertical(LineSegment line) {
        return Math.abs(line.end.x - line.start.x) < CHECK_WALL_OFFSET;
    }

    // Get the wall that would be on the left hand side of the point and direction you're going in
    private LineSegment getLeftSideWall(PointF currentPoint, Direction direction) {
        PointF currentModified, compare;
        // Set the two test points appropriately based on direction
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
        // Check if there is an intersecting line between the two test points
        List<InterceptPoint> list = mapView.calculateIntersections(currentModified, compare);
        if (!list.isEmpty()) {
            // If there is, the first of such line is the wall we're looking for
            return list.get(0).getLine();
        } else {
            // No intersection, bail out
            return null;
        }
    }

    // Get the proper end point of the wall we want
    private PointF getWantedEndPoint(LineSegment wall) {
        // As there is no guarantee to the order of the start and end points,
        // Do some checks to get the end point we actually want
        float x = 0, y = 0;
        switch (nextDirection) {
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

    // Calculates where the next user point should be and returns it
    private PointF getNextUserPoint(PointF currentPoint, PointF endPoint) {
        PointF out;
        List<InterceptPoint> list;
        /*
        General procedure is as follows:
        1. Calculate the intersections between your current point and the adjusted wall end point
        2. If there is an intersection, that means there is a corner;
           Take the closest intersect point, move off of that a bit, and set that to the next point
        3. If there is no intersection, then there is no corner;
           Move to the end of the wall, set that as the next point, and then turn accordingly
         */
        switch (nextDirection) {
            case NORTH:
                out = new PointF(currentPoint.x, endPoint.y - USER_POINT_OFFSET);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.y += USER_POINT_OFFSET;
                    nextDirection = Direction.EAST;
                } else {
                    nextDirection = Direction.WEST;
                }
                return out;
            case SOUTH:
                out = new PointF(currentPoint.x, endPoint.y + USER_POINT_OFFSET);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.y -= USER_POINT_OFFSET;
                    nextDirection = Direction.WEST;
                } else {
                    nextDirection = Direction.EAST;
                }
                return out;
            case EAST:
                out = new PointF(endPoint.x + USER_POINT_OFFSET, currentPoint.y);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.x -= USER_POINT_OFFSET;
                    nextDirection = Direction.SOUTH;
                } else {
                    nextDirection = Direction.NORTH;
                }
                return out;
            case WEST:
                out = new PointF(endPoint.x - USER_POINT_OFFSET, currentPoint.y);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.x += USER_POINT_OFFSET;
                    nextDirection = Direction.NORTH;
                } else {
                    nextDirection = Direction.SOUTH;
                }
                return out;
        }
        // Shouldn't reach here
        return null;
    }

    // Returns whether the path has been set or not
    public static boolean isPathSet() {
        return pathSet;
    }
}
