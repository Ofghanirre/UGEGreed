package fr.uge.ugegreed.http;

import java.io.IOException;

/**
 * File from the IGM M1 network resources:
 * <a href="http://igm.univ-mlv.fr/coursprogreseau/tds/td09-fr.html">Link to the resource</a>
 * Edited for the project UGEGreed
 */
public class HttpException extends IOException {

    private static final long serialVersionUID = -1810727803680020453L;

    public HttpException() {
        super();
    }

    public HttpException(String s) {
        super(s);
    }

    public static void ensure(boolean b, String string) throws HttpException {
        if (!b){
            throw new HttpException(string);
        }
    }
}
