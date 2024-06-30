/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.model;

import java.util.ArrayList;

public class ScNodeContentTable implements ScNodeContent {
    private byte type;
    private ArrayList<CharSequence[]> content;
    private int colMin; // Has no effect in SourCherry
    private int colMax; // Has no effect in SourCherry
    private String justification;
    // This variable represent is_light element of table tag in CherryTree database
    // Has no affect in SourCherry
    private byte lightInterface;
    private int newOffset;
    private String colWidths; // Has no effect in SourCherry

    /**
     * Constructor for ScNodeContent object that holds table's data of the node
     * @param type 0 - text to set into TextView, 1 - table content
     * @param content node's table content
     * @param colMin column min width
     * @param colMax column max width
     * @param lightInterface 1 - light interface is used in, 0 - otherwise
     * @param justification justification of the table and not it's content
     * @param colWidths comma separated values representing individual width of the table columns
     */
    public ScNodeContentTable(byte type, ArrayList<CharSequence[]> content, int colMin, int colMax, byte lightInterface, String justification, String colWidths) {
        this.type = type;
        this.content = content;
        this.colMin = colMin;
        this.colMax = colMax;
        this.lightInterface = lightInterface;
        this.justification = justification;
        this.colWidths = colWidths;
    }

    /**
     * Get table column max width
     * Not in use. Value most likely always the same as colMin.
     * @return table column max width
     */
    public int getColMax() {
        return this.colMax;
    }

    /**
     * Set table column max width
     * Not in use. Value most likely always the same as colMin.
     * @param colMax table column max width
     */
    public void setColMax(int colMax) {
        this.colMax = colMax;
    }

    /**
     * Get table column min width
     * @return table column min width
     */
    public int getColMin() {
        return this.colMin;
    }

    /**
     * Set table column min width
     * @param colMin table column min width
     */
    public void setColMin(int colMin) {
        this.colMin = colMin;
    }

    /**
     * Get width of every value
     * @return comma separated values of column width. 0 means user did not modify the column width and default should be used
     */
    public String getColWidths() {
        return colWidths;
    }

    /**
     * Set widths for individual table columns.
     * @param colWidths Values for each column has to be comma separated. Default value - 0, other positive value - user changed value
     */
    public void setColWidths(String colWidths) {
        this.colWidths = colWidths;
    }

    /**
     * Get node's table content
     * @return node's table content
     */
    public ArrayList<CharSequence[]> getContent() {
        return this.content;
    }

    /**
     * Set node's table content
     * @param content node's table content
     */
    public void setContent(ArrayList<CharSequence[]> content) {
        this.content = content;
    }

    @Override
    public byte getContentType() {
        return this.type;
    }

    @Override
    public void setContentType(byte type) {
        this.type = type;
    }

    /**
     * Return justification of the table. Possible values: left, center, right, fill.
     * @return justification value
     */
    public String getJustification() {
        return justification;
    }

    /**
     * Setting justification value of the table, but not it's content.
     * It is needed to save it back to the database.
     * @param justification justification value. Possible values: left, center, right, fill.
     */
    public void setJustification(String justification) {
        this.justification = justification;
    }

    /**
     * Is table table using lightweight interface
     * @return 1 - if table using lightweight interface, 0 - otherwise
     */
    public byte getLightInterface() {
        return this.lightInterface;
    }

    /**
     * Setting what interface is used by the table
     * @param lightInterface 1 - if table using lightweight interface, 0 - otherwise
     */
    public void setLightInterface(byte lightInterface) {
        this.lightInterface = lightInterface;
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
}
