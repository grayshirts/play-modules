package controllers;

import play.mvc.Controller;
import play.mvc.With;
import plugins.gzip.Compress;

@With(Compress.class)
public class Application extends Controller {

    public static void index() {
        render();
    }

}