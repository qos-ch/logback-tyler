/*
 * Copyright (c) 2024 QOS.ch Sarl (Switzerland)
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 *
 */

package ch.qos.logback.tyler.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OutputComparator {

    boolean checkForEquality(List<String> witnessList, List<String> subjectList) {
        List<String> cleanWitnessList = clean(witnessList);
        List<String> cleanSubjectList = clean(subjectList);

        final int lenWitness = cleanWitnessList.size();
        final int lenSubject = cleanSubjectList.size();

        if(lenWitness != lenSubject) {
            System.out.println("Lists of differt sizes "+ lenWitness+"!="+lenSubject);
            return false;
        }

        for(int i = 0; i < lenWitness; i++) {
            String w = cleanWitnessList.get(i).trim();
            String s = cleanSubjectList.get(i).trim();
            if(!w.equals(s)) {
                System.out.println("lists differ on line "+i);
                System.out.println("w= "+w);
                System.out.println("s= "+s);
                return false;
            }
        }

        return true;

    }

    private List<String> clean(List<String> inputList) {
        List<String> cleanList = new ArrayList<>();
        for (String s : inputList) {
            if (!toSkip(s)) {
                cleanList.add(s);
            }
        }
        return cleanList;
    }

    Pattern pattern0 = Pattern.compile("^\\s*[/]?\\*");

    private boolean toSkip(String s) {
        if (s.isBlank())
            return true;

        if (pattern0.matcher(s).find())
            return true;

        return false;
    }
}
