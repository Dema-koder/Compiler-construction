package bytecode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Param {
    private String name;
    private String type;
    private String classOwner;
    private Boolean isClassParam;
    private String methodOwner;
    private Boolean isMethodParam;
    private String localPosition;
}
