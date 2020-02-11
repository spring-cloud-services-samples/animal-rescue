import React from 'react';
import './App.css';
import AnimalCards from "./components/animal-cards";
import {Button} from "semantic-ui-react";

export default class App extends React.Component {
    signIn = () => {
      axios.get
    };
    render() {
        return (
            <div className="App">
                <header className="App-header">
                    <p>
                        Animal Rescue Center
                    </p>
                    <Button animated='fade' onClick={this.signIn}>
                        <Button.Content visible>Sign in to adopt</Button.Content>
                        <Button.Content hidden>It only takes a loving heart!</Button.Content>
                    </Button>
                </header>
                <div className={"App-body"}>
                    <AnimalCards/>
                </div>
            </div>
        );
    }
}

