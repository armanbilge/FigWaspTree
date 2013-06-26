package figtree.treeviewer.treelayouts;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import figtree.treeviewer.treelayouts.TreeLayout.AxisType;

import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;

/**
 * @author Andrew Rambaut
 * @version $Id: AbstractTreeLayout.java 819 2007-10-22 14:42:58Z rambaut $
 */
public abstract class AbstractTreeLayout implements TreeLayout {
	private double rootLength = 0.0;
    private boolean isAxisReversed;
   
    public boolean isAxisReversed() {
        return isAxisReversed;
    }

    public void setAxisReversed(final boolean axisReversed) {
        isAxisReversed = axisReversed;
    }

    public double getRootLength() {
		return rootLength;
	}

	public void setRootLength(double rootLength) {
		this.rootLength = rootLength;
		fireTreeLayoutChanged();
	}

	public void addTreeLayoutListener(TreeLayoutListener listener) {
        listeners.add(listener);
    }

    public void removeTreeLayoutListener(TreeLayoutListener listener) {
        listeners.remove(listener);
    }

	protected void fireTreeLayoutChanged() {
        for (TreeLayoutListener listener : listeners) {
            listener.treeLayoutChanged();
        }
    }

    public String getBranchColouringAttributeName() {
        return branchColouringAttribute;
    }

    public void setBranchColouringAttributeName(String branchColouringAttribute) {
        this.branchColouringAttribute = branchColouringAttribute;
        fireTreeLayoutChanged();
    }

    public String getCartoonAttributeName() {
        return cartoonAttributeName;
    }

    public void setCartoonAttributeName(String cartoonAttributeName) {
        this.cartoonAttributeName = cartoonAttributeName;
        fireTreeLayoutChanged();
    }

    public boolean isShowingCartoonTipLabels() {
        return showingCartoonTipLabels;
    }

    public void setShowingCartoonTipLabels(boolean showingCartoonTipLabels) {
        this.showingCartoonTipLabels = showingCartoonTipLabels;
        fireTreeLayoutChanged();
    }

	public String getCollapsedAttributeName() {
		return collapsedAttributeName;
	}

	public void setCollapsedAttributeName(String collapsedAttributeName) {
		this.collapsedAttributeName = collapsedAttributeName;
		fireTreeLayoutChanged();
	}

	public String getHilightAttributeName() {
		return hilightAttributeName;
	}

	public void setHilightAttributeName(String hilightAttributeName) {
		this.hilightAttributeName = hilightAttributeName;
		fireTreeLayoutChanged();
	}
	
	public void layoutDependent(RootedTree tree, TreeLayoutCache cache) {
		if (dependentTreeLayout == null)
			createDependentLayout();
		dependentTreeLayout.layout(tree, cache);
	}

	protected boolean cladogram = false;
	public void setCladogram(boolean cladogram) {
		this.cladogram = cladogram;
		if (dependentTreeLayout != null)
			dependentTreeLayout.setCladogram(cladogram);
	}
	public boolean isCladogram() {
		return cladogram;
	}
	
	public void createDependentLayout() {throw new UnsupportedOperationException();}
	
	protected AbstractTreeLayout dependentTreeLayout = null;
	
    private Set<TreeLayoutListener> listeners = new HashSet<TreeLayoutListener>();
    protected String branchColouringAttribute = null;
    protected String cartoonAttributeName = null;
    protected boolean showingCartoonTipLabels = true;

	protected String collapsedAttributeName = null;

	protected String hilightAttributeName = null;
}
