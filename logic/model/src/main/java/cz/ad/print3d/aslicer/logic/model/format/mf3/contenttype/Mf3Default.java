package cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents a "Default" element in the [Content_Types].xml file.
 *
 * <p>The {@code <Default>} element specifies a content type mapping for all parts in the
 * package that have the same file extension. This reduces the size of the content types
 * file by providing a common type for many parts (e.g., all PNG images in the package).</p>
 *
 * <p>According to the Open Packaging Conventions (OPC) specification (ECMA-376 Part 2),
 * the {@code Extension} attribute specifies the file extension for all parts that share
 * the content type specified by the {@code ContentType} attribute.</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Default {

    /**
     * The file extension for all parts that share the associated content type.
     */
    @XmlAttribute(name = "Extension")
    private String extension;

    /**
     * The media type (MIME) associated with the specified file extension.
     */
    @XmlAttribute(name = "ContentType")
    private String contentType;

    /**
     * Default constructor for JAXB unmarshalling.
     */
    public Mf3Default() {
    }

    /**
     * Constructs a {@code <Default>} content type mapping for a given extension.
     *
     * @param extension   the file extension (e.g., "png")
     * @param contentType the media type (e.g., "image/png")
     */
    public Mf3Default(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    /**
     * Returns the file extension for this default mapping.
     *
     * @return the file extension string
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Sets the file extension for this default mapping.
     *
     * @param extension the file extension string to set
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the content type (MIME) associated with this extension.
     *
     * @return the media type string
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type (MIME) associated with this extension.
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
        Mf3Default that = (Mf3Default) o;
        return java.util.Objects.equals(extension, that.extension) &&
                java.util.Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(extension, contentType);
    }

    @Override
    public String toString() {
        return "Mf3Default{" +
                "extension='" + extension + '\'' +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
