/*******************************************************************************
 * oltpbenchmark.com
 *  
 *  Project Info:  http://oltpbenchmark.com
 *  Project Members:    Carlo Curino <carlo.curino@gmail.com>
 *              Evan Jones <ej@evanjones.ca>
 *              DIFALLAH Djellel Eddine <djelleleddine.difallah@unifr.ch>
 *              Andy Pavlo <pavlo@cs.brown.edu>
 *              CUDRE-MAUROUX Philippe <philippe.cudre-mauroux@unifr.ch>  
 *                  Yang Zhang <yaaang@gmail.com> 
 * 
 *  This library is free software; you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Foundation;
 *  either version 3.0 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 ******************************************************************************/
/***************************************************************************
 *  Copyright (C) 2009 by H-Store Project                                  *
 *  Brown University                                                       *
 *  Massachusetts Institute of Technology                                  *
 *  Yale University                                                        *
 *                                                                         *
 *  Original Version:                                                      *
 *  Zhe Zhang (zhe@cs.brown.edu)                                           *
 *                                                                         *
 *  Modifications by:                                                      *
 *  Andy Pavlo (pavlo@cs.brown.edu)                                        *
 *  http://www.cs.brown.edu/~pavlo/                                        *
 *                                                                         *
 *  Permission is hereby granted, free of charge, to any person obtaining  *
 *  a copy of this software and associated documentation files (the        *
 *  "Software"), to deal in the Software without restriction, including    *
 *  without limitation the rights to use, copy, modify, merge, publish,    *
 *  distribute, sublicense, and/or sell copies of the Software, and to     *
 *  permit persons to whom the Software is furnished to do so, subject to  *
 *  the following conditions:                                              *
 *                                                                         *
 *  The above copyright notice and this permission notice shall be         *
 *  included in all copies or substantial portions of the Software.        *
 *                                                                         *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,        *
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF     *
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. *
 *  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR      *
 *  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,  *
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR  *
 *  OTHER DEALINGS IN THE SOFTWARE.                                        *
 ***************************************************************************/
package com.oltpbenchmark.tatp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.oltpbenchmark.catalog.*;

public class TATPLoader {
    private static final Logger LOG = Logger.getLogger(TATPLoader.class.getSimpleName());
    private static final boolean d = LOG.isLoggable(Level.FINE);
    
    private final long subscriberSize = 100000; // FIXME
    private final int batchSize = 10000; // FIXME
    private final double scaleFactor = 1.0; // FIXME
    private final boolean blocking = false; // FIXME
    
    private final Connection c;
    private final Map<String, Table> tables;
    
    public TATPLoader(Connection c, Map<String, Table> tables) {
    	this.c = c;
        this.tables = tables;
        if (d) LOG.fine("CONSTRUCTOR: " + TATPLoader.class.getName());
    }

    public void load() {
        if (d) LOG.fine("Starting TM1Loader [subscriberSize=" + subscriberSize + ",scaleFactor=" + scaleFactor + "]");
        
        Thread threads[] = new Thread[] {
            new Thread() {
                public void run() {
                    if (d) LOG.fine("Start loading " + TATPConstants.TABLENAME_SUBSCRIBER);
                    Table catalog_tbl = tables.get(TATPConstants.TABLENAME_SUBSCRIBER);
                    try {
                    	genSubscriber(catalog_tbl);
                    } catch (SQLException ex) {
                    	throw new RuntimeException(ex);
                    }
                    if (d) LOG.fine("Finished loading " + TATPConstants.TABLENAME_SUBSCRIBER);
                }
            },
//            new Thread() {
//                public void run() {
//                    if (d) LOG.fine("Start loading " + TATPConstants.TABLENAME_ACCESS_INFO);
//                    Table catalog_tbl = tables.get(TATPConstants.TABLENAME_ACCESS_INFO);
//                    genAccessInfo(catalog_tbl);
//                    if (d) LOG.fine("Finished loading " + TATPConstants.TABLENAME_ACCESS_INFO);
//                }
//            },
//            new Thread() {
//                public void run() {
//                    if (d) LOG.fine("Start loading " + TATPConstants.TABLENAME_SPECIAL_FACILITY + " and " + TATPConstants.TABLENAME_CALL_FORWARDING);
//                    Table catalog_spe = tables.get(TATPConstants.TABLENAME_SPECIAL_FACILITY);
//                    Table catalog_cal = tables.get(TATPConstants.TABLENAME_CALL_FORWARDING);
//                    genSpeAndCal(catalog_spe, catalog_cal);
//                    if (d) LOG.fine("Finished loading " + TATPConstants.TABLENAME_SPECIAL_FACILITY + " and " + TATPConstants.TABLENAME_CALL_FORWARDING);
//                }
//            }
        };

        try {
            for (Thread t : threads) {
                t.start();
                if (blocking)
                    t.join();
            } // FOR
            if (!blocking) {
                for (Thread t : threads)
                    t.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // System.err.println("\n" + this.dumpTableCounts());

        //if (d) LOG.fine("TM1 loader done. ");
    }

    /**
     * Populate Subscriber table per benchmark spec.
     */
    void genSubscriber(Table catalog_tbl) throws SQLException {
        long s_id = 0;
        Object row[] = new Object[catalog_tbl.getColumnCount()];

        // Disable auto-commit
        this.c.setAutoCommit(false);

        // Create a prepared statement
        PreparedStatement pstmt = this.c.prepareStatement(catalog_tbl.getInsertSQL(this.batchSize));
        
        long total = 0;
        int offset = 0;
        int batch = 0;
        while (s_id++ < subscriberSize) {
            int col = offset;
            
            pstmt.setLong(++col, new Long(s_id));
            pstmt.setString(++col, TATPUtil.padWithZero((Long) row[0]));
            
            // BIT_##
            for (int j = 0; j < 10; j++) {
            	pstmt.setByte(++col, TATPUtil.number(0, 1).byteValue());
            }
            // HEX_##
            for (int j = 0; j < 10; j++) {
            	pstmt.setByte(++col, TATPUtil.number(0, 15).byteValue());
            }
            // BYTE2_##
            for (int j = 0; j < 10; j++) {
            	pstmt.setShort(++col, TATPUtil.number(0, 255).shortValue());
            }
            // msc_location + vlr_location
            for (int j = 0; j < 2; j++) {
            	pstmt.setInt(++col, TATPUtil.number(0, Integer.MAX_VALUE).intValue());
            }
            total++;
            
            if (++batch >= this.batchSize) {
                if (d) LOG.fine(String.format("%s: %6d / %d", catalog_tbl, total, subscriberSize));
                int results[] = pstmt.executeBatch();
                assert(results != null);
                this.c.commit();
                offset = 0;
                batch = 0;
            }
        } // WHILE
        if (batch > 0) {
        	if (d) LOG.fine(String.format("%s: %6d / %d", catalog_tbl, total, subscriberSize));
            int results[] = pstmt.executeBatch();
            assert(results != null);
            this.c.commit();
        }
    }

    /**
     * Populate Access_Info table per benchmark spec.
     */
    void genAccessInfo(Table catalog_tbl) {
        // FIXME final VoltTable table = CatalogUtil.getVoltTable(catalog_tbl);
        int s_id = 0;
        int[] arr = { 1, 2, 3, 4 };

        int[] ai_types = TATPUtil.subArr(arr, 1, 4);
        long total = 0;
        while (s_id++ < subscriberSize) {
            for (int ai_type : ai_types) {
                Object row[] = new Object[catalog_tbl.getColumnCount()];
                row[0] = new Long(s_id);
                row[1] = ai_type;
                row[2] = TATPUtil.number(0, 255).intValue();
                row[3] = TATPUtil.number(0, 255).intValue();
                row[4] = TATPUtil.astring(3, 3);
                row[5] = TATPUtil.astring(5, 5);
                // FIXME table.addRow(row);
                total++;
            } // FOR
            /* FIXME
            if (table.getRowCount() >= TM1Constants.BATCH_SIZE) {
                if (d) LOG.fine(String.format("%s: %6d / %d", TM1Constants.TABLENAME_ACCESS_INFO, total, ai_types.length * subscriberSize));
                loadVoltTable(TM1Constants.TABLENAME_ACCESS_INFO, table);
                table.clearRowData();
            }
            */
        } // WHILE
        /* FIXME
        if (table.getRowCount() > 0) {
            if (d) LOG.fine(String.format("%s: %6d / %d", TM1Constants.TABLENAME_ACCESS_INFO, total, ai_types.length * subscriberSize));
            loadVoltTable(TM1Constants.TABLENAME_ACCESS_INFO, table);
            table.clearRowData();
        }
        */
    }

    /**
     * Populate Special_Facility table and CallForwarding table per benchmark
     * spec.
     */
    void genSpeAndCal(Table catalog_spe, Table catalog_cal) {
        // FIXME VoltTable speTbl = CatalogUtil.getVoltTable(catalog_spe);
        // FIXME VoltTable calTbl = CatalogUtil.getVoltTable(catalog_cal);

        int s_id = 0;
        long speTotal = 0;
        long calTotal = 0;
        int[] arrSpe = { 1, 2, 3, 4 };
        int[] arrCal = { 0, 8, 6 };

        while (s_id++ < subscriberSize) {
            int[] sf_types = TATPUtil.subArr(arrSpe, 1, 4);
            for (int sf_type : sf_types) {
                Object row[] = new Object[catalog_spe.getColumnCount()];
                row[0] = new Long(s_id);
                row[1] = sf_type;
                row[2] = TATPUtil.isActive();
                row[3] = TATPUtil.number(0, 255).intValue();
                row[4] = TATPUtil.number(0, 255).intValue();
                row[5] = TATPUtil.astring(5, 5);
                // FIXME speTbl.addRow(row);
                speTotal++;

                // now call_forwarding
                int[] start_times = TATPUtil.subArr(arrCal, 0, 3);
                for (int start_time : start_times) {
                    Object row_cal[] = new Object[catalog_cal.getColumnCount()];
                    row_cal[0] = row[0];
                    row_cal[1] = row[1];
                    row_cal[2] = start_time;
                    row_cal[3] = start_time + TATPUtil.number(1, 8);
                    row_cal[4] = TATPUtil.nstring(15, 15);
                    // FIXME calTbl.addRow(row_cal);
                    calTotal++;
                } // FOR
            } // FOR
            
            /* FIXME
            if (calTbl.getRowCount() >= TM1Constants.BATCH_SIZE) {
                if (d) LOG.fine(String.format("%s: %d", TM1Constants.TABLENAME_CALL_FORWARDING, calTotal));
                loadVoltTable(TM1Constants.TABLENAME_CALL_FORWARDING, calTbl);
                calTbl.clearRowData();
            }
            if (speTbl.getRowCount() >= TM1Constants.BATCH_SIZE) {
                if (d) LOG.fine(String.format("%s: %d", TM1Constants.TABLENAME_SPECIAL_FACILITY, speTotal));
                loadVoltTable(TM1Constants.TABLENAME_SPECIAL_FACILITY, speTbl);
                speTbl.clearRowData();
            }
            */
        } // WHILE
        /* FIXME
        if (calTbl.getRowCount() > 0) {
            if (d) LOG.fine(String.format("%s: %d", TM1Constants.TABLENAME_CALL_FORWARDING, calTotal));
            loadVoltTable(TM1Constants.TABLENAME_CALL_FORWARDING, calTbl);
            calTbl.clearRowData();
        }
        if (speTbl.getRowCount() > 0) {
            if (d) LOG.fine(String.format("%s: %d", TM1Constants.TABLENAME_SPECIAL_FACILITY, speTotal));
            loadVoltTable(TM1Constants.TABLENAME_SPECIAL_FACILITY, speTbl);
            speTbl.clearRowData();
        }
        */
    }
}