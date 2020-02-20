import React from 'react';
import './App.css';
import logo from './logo.svg';
import AnimalCards from "./components/animal-cards";
import {Button} from "semantic-ui-react";
import HttpClient from "./httpClient";

export default class App extends React.Component {

    #loginLink = process.env.REACT_APP_LOGIN_PATH || '/rescue/admin';

    constructor(props, context) {
        super(props, context);
        this.httpClient = new HttpClient();
        this.state = {
            username: '',
        };
    }

    getUsername = async () => {
        const res = await this.httpClient.getUsername();
        this.setState({username: res});
    };

    signedIn() {
        return window.location.pathname === '/rescue/admin';
    }

    componentDidMount() {
        if (this.signedIn()) {
            this.getUsername();
        }
    }

    render() {
        const whoAmI = this.state.username === '' ? (
            <Button animated='fade' color='black' onClick={this.getUsername}>
                <Button.Content visible>Who am I?</Button.Content>
                <Button.Content hidden>Let meow greet you</Button.Content>
            </Button>
        ) : (
            <Button disabled> Have a cute day {this.state.username}! </Button>
        );

        const actionButton = (this.signedIn()) ? whoAmI : (
            <Button animated='fade' color='green' href={this.#loginLink}>
                <Button.Content visible>Sign in to adopt</Button.Content>
                <Button.Content hidden>It only takes a loving heart</Button.Content>
            </Button>
        );


        return (
            <div className="App">
                <header className="App-header">
                    <img src={logo} title="Logo" width="250" alt="Logo"/>
                    <div className="header-buttons">
                        {actionButton}
                    </div>
                </header>
                <div className={"App-body"}>
                    <AnimalCards username={this.state.username}
                                 httpClient={this.httpClient}/>
                </div>
            </div>
        );
    }
}
