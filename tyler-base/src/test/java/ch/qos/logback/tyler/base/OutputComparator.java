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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputComparator {

    boolean checkForEquality(List<String> witnessList, List<String> subjectList) {
        List<String> cleanWitnessList = clean(witnessList);
        List<String> cleanSubjectList = clean(subjectList);

        //cleanSubjectList.forEach(System.out::println);

        final int lenWitness = cleanWitnessList.size();
        final int lenSubject = cleanSubjectList.size();

        if(lenWitness != lenSubject) {
            System.out.println("Lists of different sizes "+ lenWitness+"!="+lenSubject);
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
            String replacement = processLine(s);
            if(replacement!=null)
                cleanList.add(replacement);
        }
        return cleanList;
    }

    Pattern javadocPattern = Pattern.compile("^\\s*[/]?\\*");

    // 15:44:51,092 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@1f760b47 - End of configuration.
    static final Pattern STATUS_PATTERN = Pattern.compile("^\\s*\\/*\\s*[\\d:,]{12} \\|");


    private String processLine(String s) {
        if (s.isBlank())
            return null;

        if (javadocPattern.matcher(s).find()) {
            return null;
        }

        Matcher statusMatcher = STATUS_PATTERN.matcher(s);
        if(statusMatcher.find()) {
            String cleaned = statusMatcher.replaceFirst("");
            String cleaned2 = cleanMemory(cleaned);
            return cleaned2;
        }
        return s;
    }

    // ....processor.DefaultProcessor@1f760b47
    static final Pattern OBJECT_ID_PATTERN = Pattern.compile("@[\\dA-Fa-f]{6,10} ");

    private String cleanMemory(String input) {
        Matcher objectIdMatcher = OBJECT_ID_PATTERN.matcher(input);
        if(objectIdMatcher.find()) {
            String clean2 = objectIdMatcher.replaceFirst("@XXX ");
            return clean2;
        }
        return input;
    }
}
