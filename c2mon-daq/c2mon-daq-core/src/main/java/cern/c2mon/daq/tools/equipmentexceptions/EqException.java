/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.daq.tools.equipmentexceptions;

/**
 * This class represents a general Equipment exception
 */
public class EqException extends Exception {

    /** Serial version UID */
    private static final long serialVersionUID = -2595430066257117857L;

    /**
     * TODO Is this error code of any use? an error code
     */
    private int errorCode;

    /**
     * a textual description of the problem
     */
    private String errorDescription;

    /**
     * The default constructor
     */
    public EqException() {

    }
    
    public EqException(Throwable e) {
        super(e);
    }

    /**
     * Creates a new Equipment exception with the provided description and error
     * code.
     * 
     * @param error
     *            The error code of the exception.
     * @param descr
     *            The description of the exception.
     */
    public EqException(final int error, final String descr) {
        this.errorCode = error;
        this.errorDescription = descr;
    }

    /**
     * Creates a new Equipment exception with the provided error code.
     * 
     * @param error
     *            The error code of the exception.
     */
    public EqException(final int error) {
        this(error, "");
    }

    /**
     * Creates a new Equipment exception with the provided description.
     * 
     * @param descr
     *            The description of the exception.
     */
    public EqException(final String descr) {
        this(-1, descr);
    }

    public EqException(String message, Throwable e) {
       super(message, e);
       errorDescription = message;
    }

    /**
     * This method returns the error code.
     * 
     * @return The error code of the exception.
     */
    public int getErrorCode() {
        return this.errorCode;
    }

    /**
     * This method return the error description
     * 
     * @return The description of the error.
     */
    public String getErrorDescription() {
        return this.errorDescription;
    }

    @Override
    public String getMessage() {
        return this.getErrorDescription();
    }
}
