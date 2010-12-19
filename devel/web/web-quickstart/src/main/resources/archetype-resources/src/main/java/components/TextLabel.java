#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.components;

import net.contextfw.web.application.component.Attribute;
import net.contextfw.web.application.component.Component;

public class TextLabel extends Component {

    @SuppressWarnings("unused")
    @Attribute
    private String value = "";

    public void setValue(String value) {
        this.value = value;
    }
}