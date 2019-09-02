package uk.ac.soton.brm.xml;

import org.apache.tools.ant.types.resources.comparators.Date;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Testing {
    /*
        Date.compareTo(Obj) is not working as lack of consistency for returned value
     */
    static SimpleDateFormat formatter;

    public static String getTerm(Calendar calendar){
        String termStr = "";
        if (calendar.get(Calendar.MONTH) + 1 >= 8){
            Integer year = calendar.get((Calendar.YEAR));
            Integer nextY = (year + 1) % 1000;
            termStr = Integer.toString(year) + Integer.toString(nextY);
            System.out.println("new term is : " + termStr);
        } else {
            Integer year = calendar.get((Calendar.YEAR)) - 1;
            Integer nextY = (year + 1) % 1000;
            termStr = Integer.toString(year) + Integer.toString(nextY);
            System.out.println("new term is : " + termStr);
        }
        return termStr;


    }

    public static void main(String[] args){
        formatter = new SimpleDateFormat("dd-MM-yyyy");
        Calendar calendar = Calendar.getInstance();
        System.out.println(formatter.format(calendar.getTime()));

        System.out.println(getTerm(calendar));


    }
}
