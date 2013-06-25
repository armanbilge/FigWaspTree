package figtree.treeviewer.treelayouts;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;


public class RectilinearDependentTreeLayout extends RectilinearTreeLayout {

	    private double curvature = 0.0;
	    private boolean alignTipLabels = false;

	    private double fishEye = 0.0;
	    private double pointOfInterest = 0.5;
	    private int tipCount = 0;

	    private double rootLengthProportion = 0.01;

	    double yIncrement;
	    private final double Y_INCREMENT_FACTOR = 4;

	    private double maxXPosition;

	    private final RectilinearTreeLayout parentLayout;
	    public RectilinearDependentTreeLayout(RectilinearTreeLayout parentLayout) {
	    	this.parentLayout = parentLayout;
	    	copyParameters();
	    	this.parentLayout.addTreeLayoutListener(new TreeLayoutListener() {
	    		public void treeLayoutChanged() {RectilinearDependentTreeLayout.this.copyParameters();}
	    		});
	    }
	    
	    public void copyParameters() {
	    	curvature = parentLayout.getCurvature();
	    	alignTipLabels = parentLayout.isAlignTipLabels();
	    	fishEye = parentLayout.getFishEye();
	    	pointOfInterest = parentLayout.getPointOfInterest();
	    	rootLengthProportion = parentLayout.getRootLengthProportion();
	    }
	    
	    public void layout(RootedTree tree, TreeLayoutCache ...caches) {

	    	TreeLayoutCache cache = caches[0];
	    	cache.clear();
	    	TreeLayoutCache hostCache = caches[1];

	        maxXPosition = 0.0;

	        tipCount = tree.getExternalNodes().size();
	        yIncrement = (1.0 / ((hostCache.nodeIds.size() + 1) / 2)) / Y_INCREMENT_FACTOR; 

	        Node root = tree.getRootNode();
	        setRootLength(rootLengthProportion * tree.getHeight(root));

	        maxXPosition = 0.0;
	        getMaxXPosition(tree, root, getRootLength());

	        Point2D rootPoint = constructNode(tree, root, 0.0, getRootLength(), cache, hostCache);

	        constructNodeAreas(tree, root, new Area(), cache);

	        // construct a root branch line
	        double ty = transformY(rootPoint.getY());
	        Line2D line = new Line2D.Double(0.0, ty, rootPoint.getX(), ty);

	        // add the line to the map of branch paths
	        cache.branchPaths.put(root, line);

	    }

	    private Point2D constructNode(final RootedTree tree, final Node node, final double xParent, final double xPosition, TreeLayoutCache cache, TreeLayoutCache hostCache) {

        	// Use the y-position of the host branch
        	final Node hostNode = hostCache.nodeIds.get((Integer) node.getAttribute("host.nodeRef"));
        	int symbiontCount = hostCache.symbiontCounts.get(hostNode) + 1;
        	hostCache.symbiontCounts.put(hostNode, symbiontCount);
            final double yPos = hostCache.nodePoints.get(hostNode).getY() + symbiontCount * yIncrement;
	    	
	        Point2D nodePoint;

	        if (!tree.isExternal(node)) {

	            if (hilightAttributeName != null && node.getAttribute(hilightAttributeName) != null) {
	                constructHilight(tree, node, xParent, xPosition, cache);
	            }

	            if (collapsedAttributeName != null && node.getAttribute(collapsedAttributeName) != null) {
	                nodePoint = constructCollapsedNode(tree, node, xPosition, cache);
	            } else if (cartoonAttributeName != null && node.getAttribute(cartoonAttributeName) != null) {
	                nodePoint = constructCartoonNode(tree, node, xPosition, cache);
	            } else {

	                List<Node> children = tree.getChildren(node);

	                boolean rotate = false;
	                if (node.getAttribute("!rotate") != null &&
	                        ((Boolean)node.getAttribute("!rotate"))) {
	                    rotate = true;
	                }

	                for (int i = 0; i < children.size(); i++) {
	                    int index = i;
	                    if (rotate) {
	                        index = children.size() - i - 1;
	                    }
	                    Node child = children.get(index);
	                    double length = tree.getLength(child);
	                    Point2D childPoint = constructNode(tree, child, xPosition, xPosition + length, cache, hostCache);
	                }

	                nodePoint = new Point2D.Double(xPosition, yPos);
	                final double ty = transformY(yPos);

	                // start point
	                final float x0 = (float) nodePoint.getX();
	                final float y0 = (float) ty;

	                for (Node child : children) {

	                    Point2D childPoint = cache.nodePoints.get(child);

	                    GeneralPath branchPath = new GeneralPath();

	                    // end point
	                    final float x1 = (float) childPoint.getX();
	                    final float y1 = (float) transformY(childPoint.getY());

	                    if (curvature == 0.0) {
	                        Object[] colouring = null;
	                        if (branchColouringAttribute != null) {
	                            colouring = (Object[])child.getAttribute(branchColouringAttribute);
	                        }
	                        if (colouring != null) {
	                            // If there is a colouring, then we break the path up into
	                            // segments. This should allow us to iterate along the segments
	                            // and colour them as we draw them.

	                            float nodeHeight = (float) tree.getHeight(node);
	                            float childHeight = (float) tree.getHeight(child);

	                            // to help this, we are going to draw the branch backwards
	                            branchPath.moveTo(x1, y1);
	                            float x = x1;
	                            for (int i = 0; i < colouring.length - 1; i+=2) {
//									float height = ((Number)colouring[i+1]).floatValue();
//									float p = (height - childHeight) / (nodeHeight - childHeight);
	                                float interval = ((Number)colouring[i+1]).floatValue();
	                                float p = interval / (nodeHeight - childHeight);
	                                x -= ((x1 - x0) * p);
	                                branchPath.lineTo(x, y1);
	                            }
	                            branchPath.lineTo(x0, y1);
	                            branchPath.lineTo(x0, y0);
	                        } else {
	                            branchPath.moveTo(x1, y1);
	                            branchPath.lineTo(x0, y1);
	                            branchPath.lineTo(x0, y0);
	                        }
	                    } else if (curvature == 1.0) {
	                        // The extreme is to use a triangular look
	                        branchPath.moveTo(x0, y0);
	                        branchPath.lineTo(x1, y1);
	                    } else {
	                        // if the curvature is on then we simply don't
	                        // do tree colouring - I just can't be bothered to
	                        // implement it (and it would probably be confusing anyway).
	                        float x2 = x1 - ((x1 - x0) * (float) (1.0 - curvature));
	                        float y2 = y0 + ((y1 - y0) * (float) (1.0 - curvature));

	                        branchPath.moveTo(x1, y1);
	                        branchPath.lineTo(x2, y1);
	                        branchPath.quadTo(x0, y1, x0, y2);
	                        branchPath.lineTo(x0, y0);
	                    }

	                    // add the branchPath to the map of branch paths
	                    cache.branchPaths.put(child, branchPath);

	                    double x3 = (nodePoint.getX() + childPoint.getX()) / 2;
	                    Line2D branchLabelPath = new Line2D.Double(
	                            x3 - 1.0, y1,
	                            x3 + 1.0, y1);

	                    cache.branchLabelPaths.put(child, branchLabelPath);
	                }

	                Line2D nodeLabelPath = new Line2D.Double(
	                        nodePoint.getX(), ty,
	                        nodePoint.getX() + 1.0, ty);

	                cache.nodeLabelPaths.put(node, nodeLabelPath);

	                Line2D nodeShapePath = new Line2D.Double(
	                        nodePoint.getX(), ty,
	                        nodePoint.getX() - 1.0, ty);
	                cache.nodeShapePaths.put(node, nodeShapePath);
	            }
	        } else {

	            nodePoint = new Point2D.Double(xPosition, yPos);
	            double ty = transformY(yPos);

	            Line2D tipLabelPath;

	            if (alignTipLabels) {

	                tipLabelPath = new Line2D.Double(
	                        maxXPosition, ty,
	                        maxXPosition + 1.0, ty);

	                Line2D calloutPath = new Line2D.Double(
	                        nodePoint.getX(), ty,
	                        maxXPosition, ty);

	                cache.calloutPaths.put(node, calloutPath);

	            } else {
	                tipLabelPath = new Line2D.Double(
	                        nodePoint.getX(), ty,
	                        nodePoint.getX() + 1.0, ty);

	            }

	            cache.tipLabelPaths.put(node, tipLabelPath);

	            Line2D nodeShapePath = new Line2D.Double(
	                    nodePoint.getX(), ty,
	                    nodePoint.getX() - 1.0, ty);
	            cache.nodeShapePaths.put(node, nodeShapePath);

	        }

	        // add the node point to the map of node points
	        cache.nodePoints.put(node, nodePoint);

	        return nodePoint;
	    }

	    private void constructNodeAreas(final RootedTree tree, final Node node, final Area parentNodeArea, TreeLayoutCache cache) {

	        if (!tree.isExternal(node) &&
	                (collapsedAttributeName == null || node.getAttribute(collapsedAttributeName) == null) &&
	                (cartoonAttributeName == null || node.getAttribute(cartoonAttributeName) == null)) {

	            List<Node> children = tree.getChildren(node);

	            boolean rotate = false;
	            if (node.getAttribute("!rotate") != null &&
	                    ((Boolean)node.getAttribute("!rotate"))) {
	                rotate = true;
	            }

	            int index = (rotate ? children.size() - 1 : 0);
	            Node child1 = children.get(index);
	            Area childArea1 = new Area();

	            constructNodeAreas(tree, child1, childArea1, cache);

	            Rectangle2D branchBounds1 = cache.getBranchPath(child1).getBounds2D();

	            index = (rotate ? 0 : children.size() - 1);
	            Node child2 = children.get(index);
	            Area childArea2 = new Area();
	            constructNodeAreas(tree, child2, childArea2, cache);

	            Rectangle2D branchBounds2 = cache.getBranchPath(child2).getBounds2D();

	            GeneralPath nodePath = new GeneralPath();

	            // start point
	            final float x0 = (float) branchBounds1.getX();
	            final float y0 = (float) (branchBounds1.getY() + branchBounds1.getHeight());
	            nodePath.moveTo(x0, y0);

	            if (curvature == 0.0) {

	                final float y1 = (float) branchBounds1.getY();
	                nodePath.lineTo(x0, y1);

	                nodePath.lineTo((float)maxXPosition, y1);

	                final float y2 = (float) (branchBounds2.getY() + branchBounds2.getHeight());
	                nodePath.lineTo((float)maxXPosition, y2);

	                nodePath.lineTo(x0, y2);

	            } else if (curvature == 1.0) {
	                // The extreme is to use a triangular look

	                final float x1 = (float) (branchBounds1.getX() + branchBounds1.getWidth());
	                final float y1 = (float) branchBounds1.getY();
	                nodePath.lineTo(x1, y1);

	                nodePath.lineTo((float)maxXPosition, y1);

	                final float y2 = (float) (branchBounds2.getY() + branchBounds2.getHeight());
	                nodePath.lineTo((float)maxXPosition, y2);

	                final float x2 = (float) (branchBounds2.getX() + branchBounds2.getWidth());
	                nodePath.lineTo(x2, y2);
	            } else {
	                final float x1 = (float) (branchBounds1.getX() + branchBounds1.getWidth());
	                final float y1 = (float) branchBounds1.getY();

	                float x2 = x1 - ((x1 - x0) * (float) (1.0 - curvature));
	                float y2 = y0 - ((y0 - y1) * (float) (1.0 - curvature));

	                nodePath.lineTo(x0, y2);
	                nodePath.quadTo(x0, y1, x2, y1);

	                nodePath.lineTo((float)maxXPosition, y1);

	                final float y3 = (float) (branchBounds2.getY() + branchBounds2.getHeight());
	                nodePath.lineTo((float)maxXPosition, y3);

	                final float x3 = (float) (branchBounds2.getX() + branchBounds2.getWidth());
	                final float x4 = x3 - ((x3 - x0) * (float) (1.0 - curvature));
	                final float y4 = y0 + ((y3 - y0) * (float) (1.0 - curvature));

	                nodePath.lineTo(x4, y3);
	                nodePath.quadTo(x0, y3, x0, y4);
	            }

	            nodePath.lineTo(x0, y0);
	            nodePath.closePath();

	            Area nodeArea = new Area(nodePath);

	            parentNodeArea.add(nodeArea);
	            parentNodeArea.add(childArea1);
	            parentNodeArea.add(childArea2);

	            nodeArea.subtract(childArea1);
	            nodeArea.subtract(childArea2);

	            cache.nodeAreas.put(node, nodeArea);
	        }
	    }

	    private Point2D constructCartoonNode(RootedTree tree, Node node, double xPosition, TreeLayoutCache cache) {
	    	throw new UnsupportedOperationException();
//	        Point2D nodePoint;
//
//	        Object[] values = (Object[])node.getAttribute(cartoonAttributeName);
//	        int tipCount = (Integer)values[0];
//	        double tipHeight = (Double)values[1];
//	        double height = tree.getHeight(node);
//	        double maxXPos = xPosition + height - tipHeight;
//
//	        double minYPos = yPosition;
//	        yPosition += yIncrement * (tipCount - 1);
//	        double maxYPos = yPosition;
//	        yPosition += yIncrement;
//
//	        // the y-position of the node is the average of the child nodes
//	        double yPos = (maxYPos + minYPos) / 2;
//
//	        nodePoint = new Point2D.Double(xPosition, yPos);
//
//	        GeneralPath collapsedShape = new GeneralPath();
//
//	        // start point
//	        float x0 = (float)nodePoint.getX();
//	        float y0 = (float)transformY(nodePoint.getY());
//
//	        // end point
//	        float x1 = (float)maxXPos;
//	        float y1 = (float)transformY(minYPos);
//
//	        float y2 = (float)transformY(maxYPos);
//
//	        collapsedShape.moveTo(x0, y0);
//	        collapsedShape.lineTo(x1, y1);
//	        collapsedShape.lineTo(x1, y2);
//	        collapsedShape.closePath();
//
//	        // add the collapsedShape to the map of branch paths
//	        cache.collapsedShapes.put(node, collapsedShape);
//
//	        Line2D nodeLabelPath = new Line2D.Double(
//	                nodePoint.getX(), y0,
//	                nodePoint.getX() + 1.0, y0);
//
//	        cache.nodeLabelPaths.put(node, nodeLabelPath);
//
//	        Line2D nodeBarPath = new Line2D.Double(
//	                nodePoint.getX(), y0,
//	                nodePoint.getX() - 1.0, y0);
//
//	        cache.nodeShapePaths.put(node, nodeBarPath);
//
//	        if (showingCartoonTipLabels) {
//	            constructCartoonTipLabelPaths(tree, node, maxXPos, new double[] { minYPos }, cache);
//	        }
//
//	        return nodePoint;
	    }

//	    private void constructCartoonTipLabelPaths(RootedTree tree, Node node,
//	                                               double xPosition, double[] yPosition,
//	                                               TreeLayoutCache cache) {
//
//	        if (!tree.isExternal(node)) {
//	            for (Node child :  tree.getChildren(node)) {
//	                constructCartoonTipLabelPaths(tree, child, xPosition, yPosition, cache);
//	            }
//	        } else {
//
//	            Point2D nodePoint = new Point2D.Double(xPosition, yPosition[0]);
//	            double x0 = nodePoint.getX();
//	            double y0 = transformY(nodePoint.getY());
//
//	            Line2D tipLabelPath;
//
//	            if (alignTipLabels) {
//
//	                tipLabelPath = new Line2D.Double(maxXPosition, y0, maxXPosition + 1.0, y0);
//
//	                Line2D calloutPath = new Line2D.Double(x0, y0, maxXPosition, y0);
//
//	                cache.calloutPaths.put(node, calloutPath);
//
//	            } else {
//	                tipLabelPath = new Line2D.Double(x0, y0, x0 + 1.0, y0);
//
//	            }
//
//	            cache.tipLabelPaths.put(node, tipLabelPath);
//
//	            yPosition[0] += yIncrement;
//
//	        }
//	    }

	    private Point2D constructCollapsedNode(RootedTree tree, Node node, double xPosition, TreeLayoutCache cache) {
	    	throw new UnsupportedOperationException();
//	        Point2D nodePoint;
//
//	        Object[] values = (Object[])node.getAttribute(collapsedAttributeName);
//	        double tipHeight = (Double)values[1];
//	        double height = tree.getHeight(node);
//	        double maxXPos = xPosition + height - tipHeight;
//
//	        double minYPos = yPosition - (yIncrement * 0.5);
//	        double maxYPos = minYPos + yIncrement;
//	        yPosition += yIncrement;
//
//	        // the y-position of the node is the average of the child nodes
//	        double yPos = (maxYPos + minYPos) / 2;
//
//	        nodePoint = new Point2D.Double(xPosition, yPos);
//	        double ty = transformY(yPos);
//
//	        GeneralPath collapsedShape = new GeneralPath();
//
//	        // start point
//	        float x0 = (float)nodePoint.getX();
//	        float y0 = (float)ty;
//
//	        // end point
//	        float x1 = (float)maxXPos;
//	        float y1 = (float)transformY(minYPos);
//
//	        float y2 = (float)transformY(maxYPos);
//
//	        collapsedShape.moveTo(x0, y0);
//	        collapsedShape.lineTo(x1, y1);
//	        collapsedShape.lineTo(x1, y2);
//	        collapsedShape.closePath();
//
//	        // add the collapsedShape to the map of branch paths
//	        cache.collapsedShapes.put(node, collapsedShape);
//
//	        Line2D nodeLabelPath = new Line2D.Double(xPosition, ty, xPosition + 1.0, ty);
//
//	        cache.nodeLabelPaths.put(node, nodeLabelPath);
//
//	        Line2D nodeBarPath = new Line2D.Double(xPosition, ty, xPosition - 1.0, ty);
//
//	        cache.nodeShapePaths.put(node, nodeBarPath);
//
//	        Line2D tipLabelPath;
//
//	        if (alignTipLabels) {
//
//	            tipLabelPath = new Line2D.Double(
//	                    maxXPosition, ty,
//	                    maxXPosition + 1.0, ty);
//
//	            Line2D calloutPath = new Line2D.Double(
//	                    maxXPos, ty,
//	                    maxXPosition, ty);
//
//	            cache.calloutPaths.put(node, calloutPath);
//
//	        } else {
//	            tipLabelPath = new Line2D.Double(maxXPos, ty, maxXPos + 1.0, ty);
//	        }
//
//	        cache.tipLabelPaths.put(node, tipLabelPath);
//
//	        return nodePoint;
	    }

	    private void constructHilight(RootedTree tree, Node node, double xParent, double xPosition, TreeLayoutCache cache) {

	    	throw new UnsupportedOperationException();
	    	
//	        Object[] values = (Object[])node.getAttribute(hilightAttributeName);
//	        int tipCount = (Integer)values[0];
//	        double tipHeight = (Double)values[1];
//	        double height = tree.getHeight(node);
//
//	        GeneralPath hilightShape = new GeneralPath();
//
//	        float x0 = (float)((xPosition + xParent) / 2.0);
//	        float x1 = (float)(xPosition + height /*- tipHeight*/);
//	        double tmp = yPosition - (yIncrement / 2);
//	        float y0 = (float)transformY(tmp);
//	        float y1 = (float)transformY(tmp + (yIncrement * tipCount));
//
//	        hilightShape.moveTo(x0, y0);
//	        hilightShape.lineTo(x1, y0);
//	        hilightShape.lineTo(x1, y1);
//	        hilightShape.lineTo(x0, y1);
//	        hilightShape.closePath();
//
//	        // add the collapsedShape to the map of branch paths
//	        cache.hilightNodes.add(node);
//	        cache.hilightShapes.put(node, hilightShape);
	    }

	    private void getMaxXPosition(RootedTree tree, Node node, double xPosition) {

	        if (!tree.isExternal(node)) {

	            List<Node> children = tree.getChildren(node);

	            for (Node child : children) {
	                double length = tree.getLength(child);
	                getMaxXPosition(tree, child, xPosition + length);
	            }

	        } else {
	            if (xPosition > maxXPosition) {
	                maxXPosition = xPosition;
	            }
	        }
	    }

	    private double transformY(double y) {
	        if (fishEye == 0.0) {
	            return y;
	        }

	        double scale = 1.0 / (fishEye * tipCount);
	        double dist = pointOfInterest - y;
	        double min =  1.0 - (pointOfInterest / (scale + pointOfInterest));
	        double max =  1.0 - ((pointOfInterest - 1.0) / (scale - (pointOfInterest - 1.0)));

	        double c =  1.0 - (dist < 0 ? (dist / (scale - dist)) : (dist / (scale + dist)));

	        return (c - min) / (max - min);
	    }


}
