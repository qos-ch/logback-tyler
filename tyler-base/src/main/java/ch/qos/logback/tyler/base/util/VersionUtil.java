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

package ch.qos.logback.tyler.base.util;

import ch.qos.logback.core.util.EnvUtil;

import java.lang.module.ModuleDescriptor;
import java.util.Optional;

public class VersionUtil {

    static public String tylerVersion() {
        String moduleVersion = tylerVersionByModule();
        if(moduleVersion != null)
            return moduleVersion;

        Package pkg = VersionUtil.class.getPackage();
        if (pkg == null) {
            return null;
        }
        return pkg.getImplementationVersion();
    }

    /**
     * <p>Returns the current version of logback via class.getModule() or null if data is not
     * available.
     * </p>
     *
     * @since 1.3.0
     * @return current version or null if missing version data
     */
    static private String tylerVersionByModule() {
        Module module = VersionUtil.class.getModule();
        if (module == null)
            return null;

        ModuleDescriptor md = module.getDescriptor();
        if (md == null)
            return null;
        Optional<String> opt = md.rawVersion();
        return opt.orElse(null);
    }
}
