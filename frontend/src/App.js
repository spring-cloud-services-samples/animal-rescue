import React from 'react';
import './App.css';
import AnimalCards from "./components/animal-cards";

export default class App extends React.Component {
    render() {
        return (
            <div className="App">
                <header className="App-header">
                    <p>
                        Animal Rescue Center
                    </p>
                </header>
                <div className={"App-body"}>
                    <AnimalCards/>
                </div>
            </div>
        );
    }
}

