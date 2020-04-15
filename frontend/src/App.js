import React from 'react';
import {Button} from "semantic-ui-react";
import "react-responsive-carousel/lib/styles/carousel.min.css";
import './App.css';
import logo from './logo.svg';
import AnimalCards from "./components/animal-cards";
import Carousel from "./components/carousel";
import {getAnimals, getUsername} from "./httpClient";
import {AppContext} from "./AppContext";

const PENDING = 'pending', AUTHENTICATED = 'authenticated', ANONYMOUS = 'anonymous';

export default class App extends React.Component {

    #loginLink = process.env.REACT_APP_LOGIN_URI || '/rescue/login';
    #logoutLink = process.env.REACT_APP_LOGOUT_URI || '/scg-logout?redirect=/rescue';

    constructor(props, context) {
        super(props, context);
        this.state = {
            username: '',
            animals: [],
            userStatus: PENDING,
        };
    }

    fetchAnimals() {
        getAnimals().then(animals => this.setState({animals}));
    }

    getUsername = () => {
        getUsername().then(name => this.setState({
            username: name,
            userStatus: name === '' ? ANONYMOUS : AUTHENTICATED,
        }));
    };

    componentDidMount() {
        this.fetchAnimals();
        this.getUsername();
    }

    render() {
        return (
            <div className="App">
                <header className="App-header">
                    <img src={logo} title="Logo" width="250" alt="Logo"/>
                    <div className="header-buttons">
                        {this.getGreetButton()}
                        {this.getActionButton()}
                    </div>
                </header>
                <Carousel/>
                <div className={"App-body"}>
                    <AppContext.Provider value={{refresh: () => this.fetchAnimals()}}>
                        <AnimalCards username={this.state.username}
                                     animals={this.state.animals}/>
                    </AppContext.Provider>
                </div>
            </div>
        );
    }

    getGreetButton() {
        switch (this.state.userStatus) {
            case ANONYMOUS:
                return <Button disabled color='green' basic> Let meow greet ya! </Button>;
            case AUTHENTICATED:
                return <Button disabled color='green' basic> Have a cute day {this.state.username}! </Button>;
            default:
                return <div/>;
        }
    }
    getActionButton() {
        switch (this.state.userStatus) {
            case ANONYMOUS:
                return (
                    <Button animated='fade' color='green' href={this.#loginLink}>
                        <Button.Content visible>Sign in to adopt</Button.Content>
                        <Button.Content hidden>It only takes a loving heart</Button.Content>
                    </Button>
                );
            case AUTHENTICATED:
                return <Button color='green' href={this.#logoutLink}>Sign out</Button>;
            default:
                return <div/>;
        }
    }
}
