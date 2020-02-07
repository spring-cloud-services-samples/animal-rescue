import React from 'react';
import './App.css';
import AnimalCards from "./components/animal-cards";
import axios from 'axios';

class App extends React.Component{
    state = {
        animals: []
    };

    componentDidMount() {
       axios.get(`http://localhost:8080/animals`)
         .then(res => {
           const animals = res.data;
           this.setState({ animals });
         })
     }

  render() {
      return (
          <div className="App">
            <header className="App-header">
              <p>
                Animal Rescue Center
              </p>
            </header>
            <div className={"App-body"}>
              <AnimalCards animals={this.state.animals}/>
            </div>
          </div>
        );
  }
}

export default App;
