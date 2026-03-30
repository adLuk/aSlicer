package cz.ad.print3d.aslicer.logic.printer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for managing {@link Printer3D} objects.
 * <p>
 * This interface provides methods for CRUD operations on 3D printer configurations,
 * organized into groups.
 * </p>
 *
 * @author Senior Architect
 * @since 1.0.0
 */
public interface PrinterRepository {

    /**
     * Retrieves all available printer groups.
     *
     * @return a list of group names
     */
    List<String> getGroups();

    /**
     * Retrieves all printers in a specific group.
     *
     * @param groupName the name of the group
     * @return a map of printer names to their corresponding {@link Printer3D} objects
     */
    Map<String, Printer3D> getPrintersByGroup(String groupName);

    /**
     * Retrieves a specific printer by its name and group.
     *
     * @param groupName   the name of the group
     * @param printerName the name of the printer
     * @return an {@link Optional} containing the printer if found, or empty otherwise
     */
    Optional<Printer3D> getPrinter(String groupName, String printerName);

    /**
     * Saves or updates a printer in a specific group.
     *
     * @param groupName   the name of the group
     * @param printerName the name of the printer
     * @param printer     the printer configuration to save
     */
    void savePrinter(String groupName, String printerName, Printer3D printer);

    /**
     * Deletes a printer from a specific group.
     *
     * @param groupName   the name of the group
     * @param printerName the name of the printer
     * @return true if the printer was successfully deleted, false otherwise
     */
    boolean deletePrinter(String groupName, String printerName);

    /**
     * Deletes an entire group of printers.
     *
     * @param groupName the name of the group to delete
     * @return true if the group was successfully deleted, false otherwise
     */
    boolean deleteGroup(String groupName);
}
