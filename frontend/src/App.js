import React from 'react';
import './App.css';
import AnimalCards from "./components/animal-cards";
import {Button, ButtonGroup} from "semantic-ui-react";
import HttpClient from "./httpClient";

export default class App extends React.Component {
    state = {
        name: '',
    };

    signIn = async () => {
        const res = await new HttpClient().signIn();
        console.info(res)
    };

    render() {
        return (
            <div className="App">
                <header className="App-header">
                    <p>
                        Animal Rescue Center
                    </p>
                    <ButtonGroup>
                        <Button animated='fade' color='red' href={'/oauth2/authorization/sso'}>
                            <Button.Content visible>Sign in to adopt</Button.Content>
                            <Button.Content hidden>Redirect back to / by design</Button.Content>
                        </Button>
                        <Button animated='fade' color='black' onClick={this.signIn}>
                            <Button.Content visible>Who am I?</Button.Content>
                            <Button.Content hidden>XHR doesn't handle redirect correctly</Button.Content>
                        </Button>
                        <Button animated='fade' color='green' href={'/rescue/admin'}>
                            <Button.Content visible>Admin view</Button.Content>
                            <Button.Content hidden>Same app but requires login</Button.Content>
                        </Button>
                    </ButtonGroup>
                </header>
                <div className={"App-body"}>
                    <AnimalCards/>
                </div>
            </div>
        );
    }
}

