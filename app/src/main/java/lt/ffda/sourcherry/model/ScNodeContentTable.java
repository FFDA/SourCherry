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
    private int colMin;
    private int colMax;
    private String justification;

    /**
     * Constructor for ScNodeContent object that holds table's data of the node
     * @param type 0 - text to set into TextView, 1 - table content
     * @param content node's table content
     * @param colMin column min width
     * @param colMax column max width
     * @param justification justification of the table and not it's content.
     */
    public ScNodeContentTable(byte type, ArrayList<CharSequence[]> content, int colMin, int colMax, String justification) {
        this.type = type;
        this.content = content;
        this.colMin = colMin;
        this.colMax = colMax;
        this.justification = justification;
    }

    @Override
    public void setContentType(byte type) {
        this.type = type;
    }

    @Override
    public byte getContentType() {
        return this.type;
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
     * @param content
     */
    public void setContent(ArrayList<CharSequence[]> content) {
        this.content = content;
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
     * Get table column max width
     * @return table column max width
     */
    public int getColMax() {
        return this.colMax;
    }

    /**
     * Set table column max width
     * @param colMax table column max width
     */
    public void setColMax(int colMax) {
        this.colMax = colMax;
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
}
