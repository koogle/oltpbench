/***************************************************************************
 *  Copyright (C) 2012 by H-Store Project                                  *
 *  Brown University                                                       *
 *  Massachusetts Institute of Technology                                  *
 *  Yale University                                                        *
 *                                                                         *
 *  http://hstore.cs.brown.edu/                                            *
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
package com.oltpbenchmark.benchmarks.auctionmark.procedures;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.auctionmark.AuctionMarkConstants;
import com.oltpbenchmark.benchmarks.auctionmark.util.AuctionMarkUtil;

/**
 * NewComment
 * @author visawee
 */
public class NewComment extends Procedure {
	
    // -----------------------------------------------------------------
    // STATEMENTS
    // -----------------------------------------------------------------
    
    public final SQLStmt getMaxItemCommentId = new SQLStmt(
        "SELECT MAX(ic_id) " + 
        "  FROM " + AuctionMarkConstants.TABLENAME_ITEM_COMMENT + 
        " WHERE ic_i_id = ? AND ic_u_id = ?"
    );
	
    public final SQLStmt insertItemComment = new SQLStmt(
        "INSERT INTO " + AuctionMarkConstants.TABLENAME_ITEM_COMMENT + "(" +
        	"ic_id," +
        	"ic_i_id," +
        	"ic_u_id," +
        	"ic_buyer_id," +
        	"ic_question, " +
        	"ic_created," +
        	"ic_updated " +
        ") VALUES (?,?,?,?,?,?,?)"
    );
    
    public final SQLStmt updateUser = new SQLStmt(
        "UPDATE " + AuctionMarkConstants.TABLENAME_USER + " " +
           "SET u_comments = u_comments + 1, " +
           "    u_updated = ? " +
        " WHERE u_id = ?"
    );
	
    // -----------------------------------------------------------------
    // RUN METHOD
    // -----------------------------------------------------------------
    
    public Object[] run(Connection conn, Date benchmarkTimes[],
                        long item_id, long seller_id, long buyer_id, String question) throws SQLException {
        final Date currentTime = AuctionMarkUtil.getProcTimestamp(benchmarkTimes);
    	
        // Set comment_id
        long ic_id = 0;
        PreparedStatement stmt = this.getPreparedStatement(conn, getMaxItemCommentId, item_id, seller_id);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            ic_id = results.getLong(1) + 1;
        }

        this.getPreparedStatement(conn, insertItemComment, ic_id, item_id, seller_id, buyer_id, question, currentTime, currentTime).executeUpdate();
        this.getPreparedStatement(conn, updateUser, currentTime, seller_id).executeUpdate();

        // Return new ic_id
        return new Object[]{ ic_id,
                             item_id,
                             seller_id } ;
    }	
	
}