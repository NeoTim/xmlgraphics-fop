/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layoutmgr;


import org.apache.fop.fo.FObj;
import org.apache.fop.area.Area;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.MinOptMax;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.fo.properties.VerticalAlign;

import org.apache.fop.area.inline.Word;
import org.apache.fop.area.inline.Space;
import org.apache.fop.area.inline.Character;

import java.util.ListIterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * LayoutManager for lines. It builds one or more lines containing
 * inline areas generated by its sub layout managers.
 *
 * The line layout manager does the following things:
 * receives a list of inline creating layout managers
 * adds the inline areas retrieved from the child layout managers
 * finds the best line break position
 * adds complete line to parent
 * stores the starting position for each line in case of recreation
 * if ipd not changed but line contains resolved values (eg. page number), redoes from that line
 * when freeing memory, release all layout managers and inline areas before current position
 * As each child layout manager is used it gets the start, end and normal references for id area, footnotes, floats, links, colour-back properties
 * first line properties are set and used by the child when retrieving the inline area(s)
 *
 * Hyphenation is handled by asking the child to split the words then this
 * adds the hyph char. If redone then exra char ignored.
 *
 * How do we handle Unicode BIDI?
 */
public class LineLayoutManager extends AbstractLayoutManager {
    private LineInfo currentLine = null;
    private boolean bFirstLine = true;
    private MinOptMax totalIPD;
    // the following values must be set by the block
    // these are the dominant basline and lineheight values
    private int lineHeight;
    private int lead;
    private int follow;

    List lmList;
    List lines = new ArrayList();

    private LayoutPos bestPos = null;
    private MinOptMax bestIPD = null;

    static class LineInfo {
        LayoutPos startPos;
        LineArea area;
        boolean hasResolved = false;
        boolean noJustify = false;
        // footnotes, floats?
    }

    public LineLayoutManager(FObj fobjBlock, List lms, int lh, int l,
                             int f) {
        super(fobjBlock);
        lmList = lms;
        lineHeight = lh;
        lead = l;
        follow = f;
    }

    public int getContentIPD() {
        return parentLM.getContentIPD();
    }

    /**
     * Call child layout managers to generate content as long as they
     * generate inline areas. If a block-level generating LM is found,
     * finish any line being filled and return to the parent LM.
     */
    public boolean generateAreas() {
        // if a side float is added and the line contains content
        // where the ipd depends on the line width then restart
        // the line with the adjusted length

        while (curPos.lmIndex < lmList.size()) {
            LeafNodeLayoutManager curLM =
              (LeafNodeLayoutManager) lmList.get(curPos.lmIndex);
            curLM.setParentLM(this);

            LeafNodeLayoutManager nextLM = null;
            if (curPos.lmIndex + 1 < lmList.size()) {
                nextLM = (LeafNodeLayoutManager) lmList.get(
                           curPos.lmIndex + 1);
                while (nextLM.size() == 0) {
                    lmList.remove(curPos.lmIndex + 1);
                    if (curPos.lmIndex + 1 == lmList.size()) {
                        nextLM = null;
                        break;
                    }
                    nextLM = (LeafNodeLayoutManager) lmList.get(
                               curPos.lmIndex + 1);

                }
            }
            if (nextLM != null) {
                nextLM.setParentLM(this);
            }
            if (curLM.resolved()) {
                currentLine.hasResolved = true;
            }
            while (curPos.subIndex < curLM.size()) {
                InlineArea ia = curLM.get(curPos.subIndex);
                InlineArea next = null;
                if (curPos.subIndex + 1 < curLM.size()) {
                    next = curLM.get(curPos.subIndex + 1);
                } else if (curPos.lmIndex + 1 < lmList.size()) {
                    if (nextLM != null) {
                        next = nextLM.get(0);
                    }
                }
                if (currentLine != null && !currentLine.noJustify &&
                        (curPos.subIndex + 1 == curLM.size() &&
                         curPos.lmIndex + 1 == lmList.size())) {
                    currentLine.noJustify = true;
                }
                if (addChild(ia, next)) {
                    if (flush()) {
                        return true;
                    }
                }
                // flush final line in same context as other lines
                // handle last line concepts
                if (curPos.subIndex + 1 == curLM.size() &&
                        curPos.lmIndex + 1 == lmList.size()) {
                    if (flush()) {
                        return true;
                    }
                    if (curPos.subIndex + 1 == curLM.size() &&
                            curPos.lmIndex + 1 == lmList.size()) {
                        return false;
                    }

                }
                curPos.subIndex++;
            }
            curPos.lmIndex++;
            curPos.subIndex = 0;
        }
        return false;
    }

    /**
     * Align and position curLine and add it to parentContainer.
     * Set curLine to null.
     */
    public boolean flush() {
        if (currentLine != null) {
            // Adjust spacing as necessary
            adjustSpacing();
            currentLine.area.verticalAlign(lineHeight, lead, follow);

            boolean res = parentLM.addChild(currentLine.area);

            lines.add(currentLine);
            currentLine = null;
            bestPos = null;
            bestIPD = null;

            return res;
        }
        return false;
    }

    /**
     * Do the ipd adjustment for stretch areas etc.
     * Consecutive spaces need to be collapsed if possible.
     * should this be on the line area so it can finish resolved areas?
     */
    private void adjustSpacing() {
        List inlineAreas = currentLine.area.getInlineAreas();

        // group text elements to split at hyphen if available
        // remove collapsable spaces at start or end on line

        // backtrack to best position
        while (true) {
            if (curPos.lmIndex == bestPos.lmIndex &&
                    curPos.subIndex == bestPos.subIndex) {
                break;
            }

            InlineArea inline =
              (InlineArea) inlineAreas.get(inlineAreas.size() - 1);
            MinOptMax ipd = inline.getAllocationIPD();
            totalIPD.subtract(ipd);

            inlineAreas.remove(inlineAreas.size() - 1);
            currentLine.noJustify = false;

            curPos.subIndex--;
            if (curPos.subIndex == -1) {
                curPos.lmIndex--;
                LeafNodeLayoutManager curLM =
                  (LeafNodeLayoutManager) lmList.get( curPos.lmIndex);
                curPos.subIndex = curLM.size() - 1;
            }
        }


        // for justify also stretch spaces to fill
        // stretch to best match
        float percentAdjust = 0;
        boolean maxSide = false;
        int realWidth = bestIPD.opt;
        if (bestIPD.opt > parentLM.getContentIPD()) {
            if (bestIPD.opt - parentLM.getContentIPD() <
                    (bestIPD.max - bestIPD.opt)) {
                percentAdjust = (bestIPD.opt - parentLM.getContentIPD()) /
                                (float)(bestIPD.max - bestIPD.opt);
                realWidth = parentLM.getContentIPD();
            } else {
                percentAdjust = 1;
                realWidth = bestIPD.max;
            }
            maxSide = true;
        } else {
            if (parentLM.getContentIPD() - bestIPD.opt <
                    bestIPD.opt - bestIPD.min) {
                percentAdjust = (parentLM.getContentIPD() - bestIPD.opt) /
                                (float)(bestIPD.opt - bestIPD.min);
                realWidth = parentLM.getContentIPD();
            } else {
                percentAdjust = 1;
                realWidth = bestIPD.min;
            }
        }
        if (percentAdjust > 0) {
            for (Iterator iter = inlineAreas.iterator(); iter.hasNext();) {
                InlineArea inline = (InlineArea) iter.next();
                int width;
                MinOptMax iipd = inline.getAllocationIPD();
                if (!maxSide) {
                    width = iipd.opt +
                            (int)((iipd.max - iipd.opt) * percentAdjust);
                } else {
                    width = iipd.opt -
                            (int)((iipd.opt - iipd.min) * percentAdjust);
                }
                inline.setWidth(width);
            }
        }

        // don't justify lines ending with U+000A or last line
        if (/*justify && */!currentLine.noJustify &&
                realWidth != parentLM.getContentIPD()) {
            ArrayList spaces = new ArrayList();
            for (Iterator iter = inlineAreas.iterator(); iter.hasNext();) {
                InlineArea inline = (InlineArea) iter.next();
                if (inline instanceof Space /* && !((Space)inline).fixed*/) {
                    spaces.add(inline);
                }
            }
            for (Iterator iter = spaces.iterator(); iter.hasNext();) {
                Space space = (Space) iter.next();
                space.setWidth(space.getWidth() +
                               (parentLM.getContentIPD() - realWidth) /
                               spaces.size());
            }
        }

    }

    /**
     * Return current lineArea or generate a new one if necessary.
     */
    public Area getParentArea(Area childArea) {
        if (currentLine.area == null) {
            createLine();
        }
        return currentLine.area;
    }

    protected void createLine() {
        currentLine = new LineInfo();
        currentLine.startPos = curPos;
        currentLine.area = new LineArea();
        /* Set line IPD from parentArea
         * This accounts for indents. What about first line indent?
         * Should we set an "isFirst" flag on the lineArea to signal
         * that to the parent (Block) LM? That's where indent property
         * information will be managed.
         */
        Area parent = parentLM.getParentArea(currentLine.area);
        // currentLine.area.setContentIPD(parent.getContentIPD());
        // totalIPD = new MinOptMax();
        // OR???
        totalIPD = new MinOptMax();
        this.bFirstLine = false;
    }

    /**
     * Called by child LayoutManager when it has filled one of its areas.
     * See if the area will fit in the current container.
     * If so, add it.
     * This should also handle floats if childArea is an anchor.
     * @param childArea the area to add: should be an InlineArea subclass!
     */
    public boolean addChild(InlineArea inlineArea, InlineArea nextArea) {
        if (currentLine == null) {
            createLine();
        }

        // add side floats first

        int pIPD = parentLM.getContentIPD();

        currentLine.area.addInlineArea(inlineArea);
        totalIPD.add(inlineArea.getAllocationIPD());

        LayoutInfo info = inlineArea.info;
        if (info == null) {
            info = new LayoutInfo();
        }
        LayoutInfo ninfo;
        if (nextArea != null && nextArea.info != null) {
            ninfo = nextArea.info;
        } else {
            ninfo = new LayoutInfo();
        }

        // the best pos cannot be before the first area
        if (bestPos == null || bestIPD == null) {
            bestPos = new LayoutPos();
            bestPos.lmIndex = curPos.lmIndex;
            bestPos.subIndex = curPos.subIndex;
            MinOptMax imop = inlineArea.getAllocationIPD();
            bestIPD = new MinOptMax(imop.min, imop.opt, imop.max);
        } else {

            // bestPos changed only when it can break
            // before/after a space or other atomic inlines
            // check keep-with on this and next
            // since chars are optimized as words we cannot assume a
            // word is complete and therefore hyphenate or break after
            // side floats effect the available ipd but do not add to line

            if (!ninfo.keepPrev && !info.keepNext &&
                    !(info.isText && ninfo.isText)) {
                if (Math.abs(bestIPD.opt - pIPD) >
                        Math.abs(totalIPD.opt - pIPD) &&
                        (totalIPD.min <= pIPD)) {
                    bestPos.lmIndex = curPos.lmIndex;
                    bestPos.subIndex = curPos.subIndex;
                    bestIPD = new MinOptMax(totalIPD.min, totalIPD.opt,
                                            totalIPD.max);
                }
            }
        }

        // Forced line break after this area (ex. ends with LF in nowrap)
        if (info.breakAfter) {
            currentLine.noJustify = true;
            return true;
        }

        if (totalIPD.min > pIPD) {
            return true;
        }

        return false;
    }

    public boolean addChild(Area childArea) {
        return false;
    }
}

