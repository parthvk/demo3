import './App.css';
import FormInputText from './components/FormInputText';
import {useState} from "react"

function App() {

// state hook
const [values, setValues] = useState({
  label: "",
  type: "",
  defaultValue: "",
  choices: "",
  order: "",
  isChecked: true
});

const handleSubmit = (e) => {
  e.preventDefault();
}

const onChange = (e) => {
  setValues({[e.target.name]: e.target.value})
}

const handleChange = (event) => {
  setValues(event.target.checked);
};

const handleClearForm = () => {
  setValues({  label: "",
  type: "",
  defaultValue: "",
  choices: "",
  order: "",
  isChecked: true});
};
console.log(values);


  return (
    <div className="app">
      <form onSubmit={handleSubmit}>
        <h1>Field Builder</h1>

<FormInputText name= "label"
            type= "text"
            placeholder= "Sales Region"
            errorMessage="The Label field is required."
            label= "Label"
            value = {values.label}
            required 
            onChange = {onChange}/>

<FormInputText name= "defaulValue"
            type= "text"
            placeholder= "Asia"
            label= "Default Value"
            value = {values.defaultValue}
            onChange = {onChange}/>

<div>
<label htmlFor="type">Type</label>
        <select id="type" name= "type" value={values.type} onChange={onChange}>
          <option value="multiSelect">Multi-select</option>
          <option value="singleSelect">Single-select</option>
        </select>



        <label htmlFor="isChecked">
      
      A value is required
    </label>
    <input type="checkbox"  name= "isChecked" checked={values.isChecked} onChange={handleChange} />
</div>

<div className = "formInput"><label htmlFor="choices">
        List Input: </label>
        <textarea value={values.choices} name= "choices" onChange={onChange} /></div>
     
        
<div><label htmlFor="order">Order</label>
        <select id="order" name= "order" value={values.order} onChange={onChange}>
          <option value="alphabetical">Display in alphabetical</option>
          <option value="reverse">Display in Reverse alphabetical</option>
        </select></div>
        


<div><button>Submit</button><text>   or   </text><text onClick={handleClearForm}>    Cancel   </text>
</div>
        
      </form>
    </div>
  );
}

export default App;
