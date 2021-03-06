/**
 * Copyright 2010 Marko Lavikainen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.contextfw.web.application.lifecycle;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import net.contextfw.web.application.component.Component;

/**
 * Maps a component to URL making it a view component. 
 * 
 * <p>
 *  This annotation must be used in conjunction with @PageScoped annotation
 * </p>
 * <p>
 *  When component is marked as a view, the page initialization process can be
 *  followed an intercepted by implementing an interface {@link ViewComponent}. 
 *  Implementing the interface is optional.
 * </p>
 * <p>
 *  URL can be given in two formats: plain and regex. Plain urls follow <i>path-like</i>
 *  convention where using <code>*</code> it is possible to match multiple paths
 * </p>
 * <p>
 *  If URL is prefixed by <code>regex:</code> then path is matched as regular expression giving
 *  more freedom.
 * </p>
 * @see PageScoped
 * @see ViewComponent
 * 
 * @author marko
 *
 */
@Target( { TYPE })
@Retention(RUNTIME)
public @interface View {
    
    /**
     * Maps view to given set of URLs.
     */
    String[] url() default "";
    
    /**
     * Maps view to given set of URLs that are mapped to property keys.
     */
    String[] property() default "";
    /**
     * Defines the parent view for this view.
     * 
     * <p>
     *  A view may have a parent which is initialized before this view. The parent
     *  may choose not to initialize this view, if it is not seen necessary.
     * </p>
     * 
     * @see ViewComponent
     */
    Class<? extends Component> parent() default Component.class;
}