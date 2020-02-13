import React from 'react';
import './App.css';
import AnimalCards from "./components/animal-cards";
import {Button} from "semantic-ui-react";
import HttpClient from "./httpClient";

export default class App extends React.Component {
    state = {
        name: '',
    };

    signIn = async () => {
      await new HttpClient().signIn()
          .then(res => {
              console.info(res)
          })
    };

    render() {
        return (
            <div className="App">
                <header className="App-header">
                    <p>
                        Animal Rescue Center
                    </p>
                    <Button animated='fade' href={'/oauth2/authorization/sso'}>
                        <Button.Content visible>Sign in to adopt</Button.Content>
                        <Button.Content hidden>It only takes a loving heart!</Button.Content>
                    </Button>
                    <Button animated='fade' onClick={this.signIn}>
                        <Button.Content visible>Who am I?</Button.Content>
                        <Button.Content hidden>Click to see!</Button.Content>
                    </Button>
                </header>
                <div className={"App-body"}>
                    <AnimalCards/>
                </div>
            </div>
        );
    }
}

