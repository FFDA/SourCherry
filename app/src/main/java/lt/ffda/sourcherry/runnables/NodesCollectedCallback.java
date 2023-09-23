/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.runnables;

/**
 * Callback to execute commands after collecting all the nodes from Multifile database into
 * drawer_menu.xml file.
 */
public interface NodesCollectedCallback {
    /**
     * Callback method to proceed after collect of failing to collect drawer_menu.xml
     * @param result 0 - success, 1 - failed, 2 - canceled
     */
    void onNodesCollected(int result);
}
