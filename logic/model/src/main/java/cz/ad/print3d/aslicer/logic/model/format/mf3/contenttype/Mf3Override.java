package cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents an "Override" element in the [Content_Types].xml file.
 *
 * <p>The {@code <Override>} element specifies a content type for a single part in the package.
 * It is used when a specific part has a different content type than other parts with the
 * same extension, or when there is no default mapping for its extension. In a 3MF file,
 * this is typically used to define the content type of the root 3D model part, which is
 * an XML file but has a specific 3MF media type.</p>
 *
 * <p>According to the Open Packaging Conventions (OPC) specification (ECMA-376 Part 2),
 * the {@code PartName} attribute specifies the name of the part, and the {@code ContentType}
 * attribute specifies its associated media type.</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Override {

    /**
     * The unique name of the part to override the content type for.
     */
    @XmlAttribute(name = "PartName")
    private String partName;

    /**
     * The media type (MIME) associated with the specified part name.
     */
    @XmlAttribute(name = "ContentType")
    private String contentType;

    /**
     * Default constructor for JAXB unmarshalling.
     */
    public Mf3Override() {
    }

    /**
     * Constructs an {@code <Override>} content type mapping for a given part name.
     *
     * @param partName    the unique part name (e.g., "/3D/3dmodel.model")
     * @param contentType the media type (e.g., "application/vnd.ms-package.3dmanufacturing-3dmodel+xml")
     */
    public Mf3Override(String partName, String contentType) {
        this.partName = partName;
        this.contentType = contentType;
    }

    /**
     * Returns the name of the part this override applies to.
     *
     * @return the part name string (URI)
     */
    public String getPartName() {
        return partName;
    }

    /**
     * Sets the name of the part this override applies to.
     *
     * @param partName the part name string (URI) to set
     */
    public void setPartName(String partName) {
        this.partName = partName;
    }

    /**
     * Returns the content type (MIME) associated with this override.
     *
     * @return the media type string
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type (MIME) associated with this override.
     *
     * @param contentType the media type string to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Override that = (Mf3Override) o;
        return java.util.Objects.equals(partName, that.partName) &&
                java.util.Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(partName, contentType);
    }

    @Override
    public String toString() {
        return "Mf3Override{" +
                "partName='" + partName + '\'' +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
