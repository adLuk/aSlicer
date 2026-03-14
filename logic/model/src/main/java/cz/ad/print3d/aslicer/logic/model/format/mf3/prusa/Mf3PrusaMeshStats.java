package cz.ad.print3d.aslicer.logic.model.format.mf3.prusa;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents mesh statistics in a Prusa-formatted 3MF file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3PrusaMeshStats {

    /**
     * Number of edges fixed.
     */
    @XmlAttribute(name = "edges_fixed")
    private Integer edgesFixed;

    /**
     * Number of degenerate facets found.
     */
    @XmlAttribute(name = "degenerate_facets")
    private Integer degenerateFacets;

    /**
     * Number of facets removed.
     */
    @XmlAttribute(name = "facets_removed")
    private Integer facetsRemoved;

    /**
     * Number of facets reversed.
     */
    @XmlAttribute(name = "facets_reversed")
    private Integer facetsReversed;

    /**
     * Number of backwards edges found.
     */
    @XmlAttribute(name = "backwards_edges")
    private Integer backwardsEdges;

    /**
     * Default constructor for JAXB.
     */
    public Mf3PrusaMeshStats() {
    }

    /**
     * Returns the number of edges fixed.
     * @return number of edges fixed
     */
    public Integer getEdgesFixed() {
        return edgesFixed;
    }

    /**
     * Returns the number of degenerate facets.
     * @return number of degenerate facets
     */
    public Integer getDegenerateFacets() {
        return degenerateFacets;
    }

    /**
     * Returns the number of facets removed.
     * @return number of facets removed
     */
    public Integer getFacetsRemoved() {
        return facetsRemoved;
    }

    /**
     * Returns the number of facets reversed.
     * @return number of facets reversed
     */
    public Integer getFacetsReversed() {
        return facetsReversed;
    }

    /**
     * Returns the number of backwards edges.
     * @return number of backwards edges
     */
    public Integer getBackwardsEdges() {
        return backwardsEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3PrusaMeshStats that = (Mf3PrusaMeshStats) o;
        return java.util.Objects.equals(edgesFixed, that.edgesFixed) &&
                java.util.Objects.equals(degenerateFacets, that.degenerateFacets) &&
                java.util.Objects.equals(facetsRemoved, that.facetsRemoved) &&
                java.util.Objects.equals(facetsReversed, that.facetsReversed) &&
                java.util.Objects.equals(backwardsEdges, that.backwardsEdges);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(edgesFixed, degenerateFacets, facetsRemoved, facetsReversed, backwardsEdges);
    }

    @Override
    public String toString() {
        return "Mf3PrusaMeshStats{" +
                "edgesFixed=" + edgesFixed +
                ", degenerateFacets=" + degenerateFacets +
                ", facetsRemoved=" + facetsRemoved +
                ", facetsReversed=" + facetsReversed +
                ", backwardsEdges=" + backwardsEdges +
                '}';
    }
}
