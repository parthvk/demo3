import { useState } from "react";
import "./FormInputText.css"


const FormInputText = (props) => {
    const [focused, setFocused] = useState(false);
    const {label, onChange, errorMessage, id, ...inputProps} = props;
    const handleFocus = (e) => {
        setFocused(true);
    }
    return(
        <div className = "formInput">
            <label>{label}</label>
            <input {...inputProps} onChange = {onChange} onBlur = {handleFocus} focused = {focused.toString()} />
            <span>{errorMessage}</span>
        </div>
    )
}

export default FormInputText