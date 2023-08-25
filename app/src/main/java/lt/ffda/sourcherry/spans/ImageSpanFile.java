/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.spans;

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

/**
 * Span used to display file attachment. Displays only the "paper clip"
 * icon. Contains additional information about the the file.
 */
public class ImageSpanFile extends ImageSpan {
    private boolean fromDatabase = false;
    private String nodeUniqueId;
    private String filename;
    private String timestamp;
    private String originalOffset;
    private int newOffset;
    private String justification;
    private String sha256sum;

    public ImageSpanFile(@NonNull Drawable drawable, int verticalAlignment) {
        super(drawable, verticalAlignment);
    }

    /**
     * Check if span was created from the file embedded into database
     * or file was attached by the user and still need to be saved
     * to database if user chooses to do so
     * @return true - file is from database, false - otherwise
     */
    public boolean isFromDatabase() {
        return this.fromDatabase;
    }

    /**
     * Set file origin.
     * @param fromDatabase true - file is from database, false - otherwise
     */
    public void setFromDatabase(boolean fromDatabase) {
        this.fromDatabase = fromDatabase;
    }

    /**
     * Unique ID of the node that attached file belongs to
     * @return unique id of the node
     */
    public String getNodeUniqueId() {
        return this.nodeUniqueId;
    }

    /**
     * Set unique id of the node that attached file belongs to
     * @param nodeUniqueId unique id of the node
     */
    public void setNodeUniqueId(String nodeUniqueId) {
        this.nodeUniqueId = nodeUniqueId;
    }

    /**
     * Get filename of the attached file
     * @return filename of the attached file (with extension)
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Set filename of the attached file
     * @param filename filename of the attached file (with extension)
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Get timestamp of when the file was saved to the database
     * @return timestamp of when file was saved in to the database. Might be null if file was just attached but never saved to database
     */
    public String getTimestamp() {
        return this.timestamp;
    }

    /**
     * Set timestamp when file was saved to the database
     * @param timestamp timestamp of when file was saved in to the database
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get original offset of the encoded_png tag. It holds offset value
     * that was saved in the database. Used to get base64Encoded String of the
     * file when saving it in the database again.
     * @return original offset of the encoded_png tag
     */
    public String getOriginalOffset() {
        return this.originalOffset;
    }

    /**
     * Set original offset of the encoded_png tag. It has to hold offset value
     * that was saved in the database. Used to get base64Encoded String of the
     * file when saving it in the database again.
     * @param originalOffset original offset of the encoded_png tag
     */
    public void setOriginalOffset(String originalOffset) {
        this.originalOffset = originalOffset;
    }

    /**
     * Get new offset of the element. It shows location where the element has to be inserted back
     * into the node content when it is being recreated.
     * @return element's offset
     */
    public int getNewOffset() {
        return this.newOffset;
    }

    /**
     * Set new offset of the element. It has to be calculated using the location of the span in the
     * node content.
     * @param newOffset element's offset
     */
    public void setNewOffset(int newOffset) {
        this.newOffset = newOffset;
    }

    /**
     * Get justification of the element
     * @return justification of the elements
     */
    public String getJustification() {
        return this.justification;
    }

    /**
     * Set justification of the element
     * @param justification element's justification ("left", "right", "center", "fill")
     */
    public void setJustification(String justification) {
        this.justification = justification;
    }

    /**
     * Get sha256sum value of the file. It is only used in Multifile databases as a filename in filesystem
     * @return sha256sum value of the file
     */
    public String getSha256sum() {
        return sha256sum;
    }

    /**
     * Set sha256sum value of the file. It is only used in Multifile databases as a filename in filesystem
     * @param sha256sum sha256sum value of the file
     */
    public void setSha256sum(String sha256sum) {
        this.sha256sum = sha256sum;
    }
}
