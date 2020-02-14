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
                    <img src="logo.svg" title="Logo" width="250" alt="Logo"/>
                    <div className="header-buttons">
                        <Button animated='fade' href={'/rescue/admin'}>
                            <Button.Content visible>Sign in to Adopt</Button.Content>
                            <Button.Content hidden>It only takes a loving heart!</Button.Content>
                        </Button>
                    </div>
                </header>
                <div className={"App-body"}>
                    <AnimalCards/>
                </div>
            </div>
        );
    }
}

