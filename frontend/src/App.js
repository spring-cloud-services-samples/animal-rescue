import React from 'react';
import {Button} from "semantic-ui-react";
import "react-responsive-carousel/lib/styles/carousel.min.css";
import './App.css';
import logo from './logo.svg';
import AnimalCards from "./components/animal-cards";
import Carousel from "./components/carousel";
import ChatSidebar from "./components/chat-sidebar";
import {getAnimals, getUsername} from "./httpClient";
import {AppContext} from "./AppContext";

const PENDING = 'pending', AUTHENTICATED = 'authenticated', ANONYMOUS = 'anonymous';

let nextMessageId = 1;

export default class App extends React.Component {

    #loginLink = process.env.REACT_APP_LOGIN_URI || '/rescue/login';
    #logoutLink = process.env.REACT_APP_LOGOUT_URI || '/scg-logout?redirect=/rescue';

    constructor(props, context) {
        super(props, context);
        this.state = {
            username: '',
            animals: [],
            userStatus: PENDING,
            chatMessages: [],
            isSidebarOpen: false,
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

    toggleSidebar = () => {
        this.setState(prev => ({isSidebarOpen: !prev.isSidebarOpen}));
    };

    addChatMessage = (text) => {
        const userMsg = {
            id: nextMessageId++,
            text,
            sender: 'user',
            timestamp: new Date(),
        };
        const botMsg = {
            id: nextMessageId++,
            text: 'Thanks for your question! A volunteer will get back to you soon.',
            sender: 'assistant',
            timestamp: new Date(),
        };
        this.setState(prev => ({
            chatMessages: [...prev.chatMessages, userMsg, botMsg],
        }));
    };

    componentDidMount() {
        this.fetchAnimals();
        this.getUsername();
    }

    render() {
        return (
            <div className={`App ${this.state.isSidebarOpen ? 'sidebar-open' : ''}`}>
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
                <ChatSidebar
                    isOpen={this.state.isSidebarOpen}
                    messages={this.state.chatMessages}
                    onSendMessage={this.addChatMessage}
                    onClose={this.toggleSidebar}
                    onOpen={this.toggleSidebar}
                />
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
