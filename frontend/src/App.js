import React from 'react';
import {Button} from "semantic-ui-react";
import "react-responsive-carousel/lib/styles/carousel.min.css";
import './App.css';
import logo from './logo.svg';
import AnimalCards from "./components/animal-cards";
import Carousel from "./components/carousel";
import {getAnimals, getUsername} from "./httpClient";
import {AppContext} from "./AppContext";

export default class App extends React.Component {

    #loginLink = process.env.REACT_APP_LOGIN_PATH || '/rescue/admin';

    constructor(props, context) {
        super(props, context);
        this.state = {
            username: '',
            animals: [],
        };
    }

    fetchAnimals() {
        getAnimals().then(animals => this.setState({animals}));
    }

    getUsername = async () => {
        const res = await getUsername();
        this.setState({username: res});
    };

    signedIn() {
        return window.location.pathname === '/rescue/admin';
    }

    componentDidMount() {
        if (this.signedIn()) {
            this.getUsername();
        }

        this.fetchAnimals();
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
}
