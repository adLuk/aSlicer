/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

    /**
     * Compares this mesh statistics with another for equality.
     *
     * @param o the object to compare with
     * @return true if both have identical stats
     */
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

    /**
     * Returns the hash code for this mesh statistics.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(edgesFixed, degenerateFacets, facetsRemoved, facetsReversed, backwardsEdges);
    }

    /**
     * Returns a string representation of this mesh statistics.
     *
     * @return a string containing stats values
     */
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
